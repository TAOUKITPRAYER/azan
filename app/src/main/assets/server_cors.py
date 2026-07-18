"""
Serveur HTTP local avec header CORS  (Access-Control-Allow-Origin: *)
Nécessaire pour que l'app Tawkit puisse charger les MP3 depuis file://
sans être bloquée par la politique cross-origin du navigateur.

Améliorations v2 :
  - ThreadingHTTPServer   : chaque connexion dans son propre thread daemon.
                            Résout le problème mono-thread de HTTPServer qui
                            causait des SRC_NOT_SUPPORTED transitoires quand
                            le probe JS et les requêtes audio arrivaient
                            simultanément.
  - HTTP 206 Partial Content (Range requests) : indispensable pour l'audio
                            HTML5. Sans ça, le navigateur envoie un Range,
                            reçoit un 200 complet, abandonne la connexion
                            (ConnectionAbortedError) et ne peut pas faire
                            de seek dans le fichier.
  - GET /health           : réponse vide 200, utilisée par le probe JS.
                            Évite de servir le listing HTML (~gros) à chaque
                            sonde, ce qui saturait le thread unique.
  - Suppression silencieuse des ConnectionAbortedError / BrokenPipeError
                            normales (client ferme après avoir bufférisé).

Usage : python server_cors.py [port] [dossier]
  port    : défaut 8080
  dossier : défaut . (dossier courant)
"""

import sys
import os
import re
from http.server import HTTPServer, SimpleHTTPRequestHandler
from socketserver import ThreadingMixIn


# ── Serveur multi-thread ──────────────────────────────────────────────────────
class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
    """HTTPServer multi-thread : chaque connexion dans son propre thread daemon.

    ThreadingMixIn fait spawn un thread par requête → les connexions audio
    longues ou les probes qui s'éternisent n'bloquent plus les autres clients.
    daemon_threads = True : les threads enfants s'arrêtent proprement avec le
    process principal (Ctrl+C).
    """
    daemon_threads = True


# ── Handler ───────────────────────────────────────────────────────────────────
class CORSRangeHandler(SimpleHTTPRequestHandler):
    """
    SimpleHTTPRequestHandler étendu avec :
      · CORS sur toutes les réponses (Access-Control-Allow-Origin: *)
      · Accept-Ranges: bytes annoncé systématiquement
      · HTTP 206 Partial Content sur requêtes avec header Range
      · GET|HEAD /health : probe léger (corps vide, pas de listing)
      · Suppression silencieuse des erreurs de connexion normales
    """

    # ── En-têtes communs ──────────────────────────────────────────────────
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Range')
        self.send_header('Accept-Ranges', 'bytes')
        super().end_headers()

    # ── Preflight CORS ────────────────────────────────────────────────────
    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    # ── Health check (probe JS) ───────────────────────────────────────────
    #
    # Le probe JS fait  fetch(serverUrl + '/health', {mode:'no-cors'}).
    # On répond avec un 200 + corps vide en quelques octets, sans générer
    # de listing HTML, ce qui élimine le "ConnectionAbortedError" parasite
    # que provoquait l'abandon du gros listing par AbortSignal.timeout.
    def do_HEAD(self):
        if self.path in ('/health', '/health/'):
            self.send_response(200)
            self.send_header('Content-Length', '0')
            self.end_headers()
            return
        super().do_HEAD()

    # ── GET avec support Range ────────────────────────────────────────────
    def do_GET(self):
        # Health check léger (même logique que HEAD)
        if self.path in ('/health', '/health/'):
            self.send_response(200)
            self.send_header('Content-Length', '0')
            self.end_headers()
            return

        # Résoudre le chemin local
        path = self.translate_path(self.path)

        # Dossier → laisser SimpleHTTPRequestHandler générer le listing
        if os.path.isdir(path):
            super().do_GET()
            return

        # Fichier introuvable
        if not os.path.isfile(path):
            self.send_error(404, 'File not found')
            return

        file_size  = os.path.getsize(path)
        range_hdr  = self.headers.get('Range')

        # ── Requête partielle : Range: bytes=start-end ────────────────────
        #
        # Le navigateur envoie typiquement "Range: bytes=0-" pour démarrer
        # le streaming, puis des ranges précis pour le seek.
        # On répond 206 + Content-Range, ce qui évite :
        #   · le transfert du fichier entier (économie réseau)
        #   · le ConnectionAbortedError (browser n'a plus besoin d'avorter)
        #   · l'impossibilité de seek dans le player audio
        if range_hdr:
            m = re.match(r'bytes=(\d+)-(\d*)', range_hdr)
            if m:
                start  = int(m.group(1))
                end    = int(m.group(2)) if m.group(2) else file_size - 1
                end    = min(end, file_size - 1)
                length = end - start + 1

                ctype = self.guess_type(path)
                try:
                    f = open(path, 'rb')
                except OSError:
                    self.send_error(500, 'Cannot open file')
                    return

                self.send_response(206)
                self.send_header('Content-Type', ctype)
                self.send_header('Content-Length', str(length))
                self.send_header('Content-Range',
                                 f'bytes {start}-{end}/{file_size}')
                self.end_headers()

                try:
                    f.seek(start)
                    remaining = length
                    while remaining > 0:
                        chunk = f.read(min(65536, remaining))
                        if not chunk:
                            break
                        self.wfile.write(chunk)
                        remaining -= len(chunk)
                except (ConnectionAbortedError, BrokenPipeError):
                    # Client a fermé la connexion après avoir bufférisé —
                    # comportement normal pour l'audio, pas une erreur.
                    pass
                finally:
                    f.close()
                return

        # ── Requête complète (pas de header Range) ────────────────────────
        super().do_GET()

    # ── Log enrichi ──────────────────────────────────────────────────────
    #
    # Affiche le header Range s'il est présent, pour faciliter le debug.
    # Les ConnectionAbortedError et BrokenPipeError sont supprimées car
    # elles sont normales (browser ferme après buffering) et polluent les logs.
    def log_message(self, fmt, *args):
        try:
            range_h = self.headers.get('Range', '') if self.headers else ''
        except Exception:
            range_h = ''
        suffix = f'  [{range_h}]' if range_h else ''
        print(f"  {self.address_string()}  {fmt % args}{suffix}")

    def log_error(self, fmt, *args):
        msg = fmt % args
        # Ignorer les erreurs de connexion normales (browser ferme la socket)
        if any(x in msg for x in ('ConnectionAbortedError', 'BrokenPipeError',
                                   'WinError 10053', 'WinError 10054',
                                   'ConnectionResetError')):
            return
        print(f"  [ERREUR] {msg}")


# ── Point d'entrée ────────────────────────────────────────────────────────────
if __name__ == '__main__':
    port      = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    directory = sys.argv[2]      if len(sys.argv) > 2 else '.'
    directory = os.path.abspath(directory)

    os.chdir(directory)

    server = ThreadingHTTPServer(('', port), CORSRangeHandler)

    print(f"\n  Serveur CORS+Range démarre  (multi-thread)")
    print(f"  Dossier  : {directory}")
    print(f"  URL      : http://localhost:{port}")
    print(f"  Headers  : Access-Control-Allow-Origin: *  |  Accept-Ranges: bytes")
    print(f"  Health   : http://localhost:{port}/health")
    print(f"  (Ctrl+C pour arrêter)\n")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n  Serveur arrêté.")
