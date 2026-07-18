import os
import zipfile
from xml.sax.saxutils import escape


OUTPUT_FILE = "TAWKIT_guide_utilisateur_ar.docx"


def mm_to_twips(mm: float) -> int:
    return int(round(mm * 56.6929133858))


def pt_to_half_points(pt: int) -> int:
    return pt * 2


def xml_text(text: str) -> str:
    return escape(text).replace("\n", "</w:t><w:br/><w:t>")


def rtl_paragraph(text: str, size_pt=26, bold=False, align="right", spacing_after=110, style=None):
    jc = {
        "right": "right",
        "center": "center",
        "justify": "both",
    }[align]
    style_xml = f'<w:pStyle w:val="{style}"/>' if style else ""
    bold_xml = "<w:b/>" if bold else ""
    return f"""
    <w:p>
      <w:pPr>
        {style_xml}
        <w:bidi/>
        <w:jc w:val="{jc}"/>
        <w:spacing w:after="{spacing_after}" w:line="360" w:lineRule="auto"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rtl/>
          <w:lang w:val="ar-TN" w:bidi="ar-TN"/>
          <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
          <w:sz w:val="{pt_to_half_points(size_pt)}"/>
          <w:szCs w:val="{pt_to_half_points(size_pt)}"/>
          {bold_xml}
        </w:rPr>
        <w:t xml:space="preserve">{xml_text(text)}</w:t>
      </w:r>
    </w:p>
    """


def build_document_xml() -> str:
    sections = [
        ("title", "دليل المستخدم المختصر للتطبيق", 20, True, "center", 80),
        ("subtitle", "TAWKIT", 22, True, "center", 120),
        ("body", "هذا الدليل باللغة العربية يعرّف المستخدمين بأهم وظائف تطبيق TAWKIT بطريقة عملية وواضحة. صُمّم التطبيق لعرض أوقات الصلاة داخل المسجد أو في البيت، مع إمكانيات تساعد على التنظيم الجيد للأذان والإقامة، وإظهار الرسائل والأذكار، وتخصيص شكل الشاشة بما يناسب المكان.", 13, False, "justify", 120),

        ("heading", "1. ما هو تطبيق TAWKIT؟", 15, True, "right", 70),
        ("body", "TAWKIT هو تطبيق مخصّص لعرض أوقات الصلوات اليومية بشكل واضح على الشاشة، مع التركيز على احتياجات المسجد: توقيت الأذان، توقيت الإقامة، العداد التنازلي للصلاة القادمة، الرسائل والإعلانات، والأوضاع البصرية المناسبة لعرض دائم أمام المصلين. كما يمكن استخدامه في المنزل أو في أي مكان يحتاج إلى شاشة مواقيت منظمة وسهلة القراءة.", 13, False, "justify", 110),

        ("heading", "2. الواجهة الرئيسية", 15, True, "right", 70),
        ("body", "عند فتح التطبيق تظهر الواجهة الرئيسية التي تتضمن عادة: اسم المسجد، التاريخ الميلادي والهجري، الساعة الحالية، أوقات الصلوات، وقت الإقامة، الوقت المتبقي للصلاة القادمة، وبيانات إضافية مثل الشروق أو الضحى أو حالة الطقس حسب الإعدادات المفعّلة. يمكن أن يعمل التطبيق بوضع أفقي أو عمودي وفق طبيعة الشاشة المستخدمة.", 13, False, "justify", 110),

        ("heading", "3. أهم الوظائف الأساسية", 15, True, "right", 70),
        ("body", "عرض أوقات الصلاة: يعرض التطبيق الفجر، الشروق، الظهر، العصر، المغرب، والعشاء بصورة واضحة وكبيرة ليسهل قراءتها من مسافة بعيدة.", 13, False, "justify", 50),
        ("body", "عرض أوقات الإقامة: يمكن إظهار وقت الإقامة بجانب كل صلاة حتى يعرف المصلون الفرق بين الأذان وموعد بدء الصلاة جماعة.", 13, False, "justify", 50),
        ("body", "العد التنازلي للصلاة القادمة: يوضّح التطبيق الزمن المتبقي للصلاة التالية، وهي ميزة مهمة لتنظيم حضور المصلين واستعداد المؤذن والإمام.", 13, False, "justify", 50),
        ("body", "شاشة الأذان وشاشة الإقامة: يستطيع التطبيق إظهار شاشة مخصصة عند الأذان، ثم شاشة أخرى مرتبطة بالإقامة أو عدّاد الثواني والدقائق قبل بدء الصلاة.", 13, False, "justify", 50),
        ("body", "إخفاء الشاشة أثناء الصلاة: يمكن تفعيل شاشة سوداء أو وضع هادئ أثناء الصلاة لتقليل التشويش داخل المسجد.", 13, False, "justify", 110),

        ("heading", "4. إعداد الموقع والتوقيت", 15, True, "right", 70),
        ("body", "من أهم خطوات الإعداد الأولى تحديد بلد ومدينة المسجد، لأن ذلك يؤثر مباشرة في حساب أوقات الصلاة. كما يمكن تعديل بعض الأوقات يدويًا إذا كانت إدارة المسجد تعتمد فارقًا محليًا خاصًا لبعض الصلوات أو ترغب في ضبط دقائق إضافية قبل أو بعد الوقت المحسوب.", 13, False, "justify", 110),
        ("body", "يتيح التطبيق أيضًا تعديل التاريخ الهجري عند الحاجة، وهي وظيفة مفيدة إذا كان المسجد يعتمد رؤية محلية أو تقويمًا معتمدًا يحتاج إلى تقديم أو تأخير بسيط. وفي بعض النسخ المتقدمة يمكن تفعيل مزامنة التاريخ الهجري أو استخدام إعدادات خاصة برمضان والعيدين.", 13, False, "justify", 110),

        ("heading", "5. الأذان، الإقامة والتنبيهات الصوتية", 15, True, "right", 70),
        ("body", "يمكن تشغيل صوت الأذان الكامل أو المختصر، وكذلك اختيار تنبيهات صوتية مرتبطة بالإقامة أو بالدقيقة الأخيرة قبل بدء الصلاة. هذه الخصائص مهمة عند استخدام التطبيق على شاشة كبيرة داخل المسجد أو في قاعة جانبية يحتاج فيها المصلون إلى تذكير سمعي واضح.", 13, False, "justify", 110),
        ("body", "كما يمكن تخصيص بعض الحالات مثل أذان الفجر الأول، إظهار حديث قبل الإقامة، أو تغيير لون العداد في الثواني الأخيرة لزيادة الانتباه. هذه الإعدادات تجعل التطبيق أكثر فاعلية في إدارة لحظة الانتقال من الانتظار إلى إقامة الصلاة.", 13, False, "justify", 110),

        ("heading", "6. الرسائل، الإعلانات والأذكار", 15, True, "right", 70),
        ("body", "يوفر TAWKIT إمكانية عرض رسائل أسفل الشاشة أو ضمن شريط متحرك، مثل إعلان درس، تذكير بموعد نشاط، أو تنبيه خاص بالمصلين. ويمكن تحرير هذه الرسائل بشكل مباشر لتناسب احتياجات المسجد اليومية أو الأسبوعية.", 13, False, "justify", 110),
        ("body", "ويدعم التطبيق كذلك عرض الأذكار بعد الصلوات، وأذكار الصباح بعد الفجر، وأذكار المساء بعد العصر أو المغرب أو العشاء حسب الإعدادات المختارة. هذا يفيد في تحويل الشاشة من مجرد مواقيت إلى وسيلة تذكير تربوية وروحية مستمرة.", 13, False, "justify", 110),
        ("body", "إضافة إلى ذلك، يمكن استخدام الشرائح أو الصور المعروضة على الشاشة لعرض محتوى دعوي أو تنظيمي، مع إمكانية تحديد مدة العرض أو تشغيل الشرائح بشكل عشوائي.", 13, False, "justify", 110),

        ("heading", "7. التخصيص والمظهر", 15, True, "right", 70),
        ("body", "يسمح التطبيق بتغيير الخلفيات، الخطوط، موضع بعض العناصر، وحجم المؤقت أو الأوقات المعروضة. ويمكن اختيار نمط عرض أفقي أو عمودي، وإظهار أو إخفاء بعض العناصر مثل أوقات الإقامة في وضع الاستعمال المنزلي. كما توجد خيارات مرتبطة بالشفافية، التظليل، وتغيير الخلفية يدويًا أو بشكل يومي أو عند كل صلاة.", 13, False, "justify", 110),
        ("body", "هذه المرونة تساعد على ملاءمة التطبيق مع شاشة تلفاز كبيرة، أو شاشة عمودية في مدخل المسجد، أو حتى شاشة صغيرة داخل مكتب الإدارة أو المنزل.", 13, False, "justify", 110),

        ("heading", "8. وظائف إضافية مفيدة", 15, True, "right", 70),
        ("body", "من الوظائف الإضافية المهمة التي قد يستفيد منها المستخدم: عرض حالة الطقس، عرض أوقات الليل مثل منتصف الليل والثلث الأخير، إظهار وقت صلاة العيد، إدراج تنبيه بعدد الأيام المتبقية لرمضان، وتصدير أو استيراد الإعدادات عند الحاجة إلى نقل التهيئة من جهاز إلى آخر.", 13, False, "justify", 110),
        ("body", "ويمكن أيضًا إدارة ملفات شخصية أو صور خاصة لاستعمالها كخلفيات أو عناصر عرض، مما يجعل التطبيق مناسبًا للمساجد التي ترغب في تخصيص الشاشة بأسلوبها البصري الخاص.", 13, False, "justify", 110),

        ("heading", "9. نصائح عملية للاستعمال اليومي", 15, True, "right", 70),
        ("body", "بعد التثبيت الأول، يُفضّل تنفيذ الخطوات التالية: تحديد اسم المسجد، اختيار المدينة الصحيحة، مراجعة أوقات الإقامة، اختبار صوت الأذان، ثم تفعيل الرسائل أو الأذكار حسب الحاجة. بعد ذلك يستحسن ترك التطبيق يعمل على شاشة ثابتة مع التأكد من وضوح الخط والحجم من آخر صف في المصلى.", 13, False, "justify", 110),
        ("body", "إذا كان الجهاز محدود الإمكانيات، فمن الأفضل تقليل عدد المؤثرات البصرية، وعدم الإكثار من الصور المتحركة أو الملفات الشخصية الثقيلة، حتى يبقى العرض سلسًا وثابتًا طوال اليوم.", 13, False, "justify", 110),

        ("heading", "10. خلاصة", 15, True, "right", 70),
        ("body", "يُعد TAWKIT تطبيقًا عمليًا وشاملًا لإدارة شاشة مواقيت الصلاة وعرض المعلومات المهمة داخل المسجد. قوته الأساسية تكمن في الجمع بين وضوح المواقيت، سهولة ضبط الإقامة، إمكانيات التنبيه، وعرض المحتوى الإرشادي مثل الرسائل والأذكار. ولهذا فهو مناسب للمستخدم الذي يبحث عن حل بسيط في المظهر، غني في الوظائف، وسهل التخصيص حسب احتياجات المسجد أو البيت.", 13, False, "justify", 110),

        ("body", "يمكن استخدام هذا الدليل كمستند تعريفي للمستخدمين الجدد أو طباعته وتوزيعه بجانب شاشة الإعداد أو داخل ملف الدعم الداخلي للتطبيق.", 13, False, "justify", 0),
    ]

    body = []
    for style, text, size, bold, align, spacing_after in sections:
        style_name = None
        if style == "title":
            style_name = "Title"
        elif style == "subtitle":
            style_name = "Subtitle"
        elif style == "heading":
            style_name = "Heading1"
        body.append(rtl_paragraph(text, size_pt=size, bold=bold, align=align, spacing_after=spacing_after, style=style_name))

    sect_pr = f"""
    <w:sectPr>
      <w:pgSz w:w="{mm_to_twips(210)}" w:h="{mm_to_twips(297)}"/>
      <w:pgMar w:top="{mm_to_twips(18)}" w:right="{mm_to_twips(18)}" w:bottom="{mm_to_twips(18)}" w:left="{mm_to_twips(18)}" w:header="{mm_to_twips(12)}" w:footer="{mm_to_twips(12)}" w:gutter="0"/>
      <w:cols w:space="720"/>
      <w:docGrid w:linePitch="360"/>
    </w:sectPr>
    """

    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas"
 xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
 xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
 xmlns:v="urn:schemas-microsoft-com:vml"
 xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing"
 xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
 xmlns:w10="urn:schemas-microsoft-com:office:word"
 xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
 xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
 xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup"
 xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk"
 xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml"
 xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape"
 mc:Ignorable="w14 wp14">
  <w:body>
    {''.join(body)}
    {sect_pr}
  </w:body>
</w:document>"""


def build_styles_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
        <w:lang w:val="ar-TN" w:bidi="ar-TN"/>
        <w:sz w:val="26"/>
        <w:szCs w:val="26"/>
      </w:rPr>
    </w:rPrDefault>
    <w:pPrDefault>
      <w:pPr>
        <w:bidi/>
        <w:jc w:val="both"/>
      </w:pPr>
    </w:pPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
    <w:pPr>
      <w:bidi/>
      <w:jc w:val="both"/>
      <w:spacing w:after="110" w:line="360" w:lineRule="auto"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
      <w:lang w:val="ar-TN" w:bidi="ar-TN"/>
      <w:sz w:val="26"/>
      <w:szCs w:val="26"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Title">
    <w:name w:val="Title"/>
    <w:basedOn w:val="Normal"/>
    <w:qFormat/>
    <w:pPr>
      <w:bidi/>
      <w:jc w:val="center"/>
      <w:spacing w:after="80" w:line="360" w:lineRule="auto"/>
    </w:pPr>
    <w:rPr>
      <w:b/>
      <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
      <w:sz w:val="40"/>
      <w:szCs w:val="40"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Subtitle">
    <w:name w:val="Subtitle"/>
    <w:basedOn w:val="Normal"/>
    <w:qFormat/>
    <w:pPr>
      <w:bidi/>
      <w:jc w:val="center"/>
      <w:spacing w:after="120" w:line="360" w:lineRule="auto"/>
    </w:pPr>
    <w:rPr>
      <w:b/>
      <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
      <w:sz w:val="44"/>
      <w:szCs w:val="44"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:basedOn w:val="Normal"/>
    <w:uiPriority w:val="9"/>
    <w:qFormat/>
    <w:pPr>
      <w:bidi/>
      <w:jc w:val="right"/>
      <w:spacing w:before="70" w:after="70" w:line="360" w:lineRule="auto"/>
      <w:outlineLvl w:val="0"/>
    </w:pPr>
    <w:rPr>
      <w:b/>
      <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
      <w:sz w:val="30"/>
      <w:szCs w:val="30"/>
    </w:rPr>
  </w:style>
</w:styles>"""


def build_content_types_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>"""


def build_root_rels_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""


def build_document_rels_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""


def build_core_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
 xmlns:dc="http://purl.org/dc/elements/1.1/"
 xmlns:dcterms="http://purl.org/dc/terms/"
 xmlns:dcmitype="http://purl.org/dc/dcmitype/"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>TAWKIT - دليل المستخدم العربي</dc:title>
  <dc:subject>دليل وظائف التطبيق</dc:subject>
  <dc:creator>OpenAI Codex</dc:creator>
  <cp:keywords>TAWKIT, Arabic, Guide</cp:keywords>
  <dc:description>دليل عربي مختصر ومقروء لأهم وظائف تطبيق TAWKIT.</dc:description>
  <cp:lastModifiedBy>OpenAI Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">2026-04-21T00:00:00Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">2026-04-21T00:00:00Z</dcterms:modified>
</cp:coreProperties>"""


def build_app_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
 xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Microsoft Office Word</Application>
  <DocSecurity>0</DocSecurity>
  <ScaleCrop>false</ScaleCrop>
  <HeadingPairs>
    <vt:vector size="2" baseType="variant">
      <vt:variant><vt:lpstr>Title</vt:lpstr></vt:variant>
      <vt:variant><vt:i4>1</vt:i4></vt:variant>
    </vt:vector>
  </HeadingPairs>
  <TitlesOfParts>
    <vt:vector size="1" baseType="lpstr">
      <vt:lpstr>Document</vt:lpstr>
    </vt:vector>
  </TitlesOfParts>
  <Company></Company>
  <LinksUpToDate>false</LinksUpToDate>
  <SharedDoc>false</SharedDoc>
  <HyperlinksChanged>false</HyperlinksChanged>
  <AppVersion>16.0000</AppVersion>
</Properties>"""


def write_docx(output_path: str) -> None:
    with zipfile.ZipFile(output_path, "w", compression=zipfile.ZIP_DEFLATED) as docx:
        docx.writestr("[Content_Types].xml", build_content_types_xml())
        docx.writestr("_rels/.rels", build_root_rels_xml())
        docx.writestr("word/document.xml", build_document_xml())
        docx.writestr("word/styles.xml", build_styles_xml())
        docx.writestr("word/_rels/document.xml.rels", build_document_rels_xml())
        docx.writestr("docProps/core.xml", build_core_xml())
        docx.writestr("docProps/app.xml", build_app_xml())


if __name__ == "__main__":
    target = os.path.join(os.getcwd(), OUTPUT_FILE)
    write_docx(target)
    print(target)
