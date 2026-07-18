let isTestModeActive = false;
const userAgentString = navigator.userAgent;
const isTawkitApp = (userAgentString.indexOf('Chagdali_Tawkit_App_4Android') !== -1);

document.documentElement.lang = JS_DATA.ucLangNOW;
const rootElement	= document.documentElement;

let versionNumberString = '10542137198121498519712148190046712111971213805784';
let encodedString1 = '19712149851971214819712129197121498419712149881971';
	encodedString1+= '21498519712148197121171971214819712111971211201971';
	encodedString1+= '21481971212919712149862019712148197121171971214984';
	encodedString1+= '19712148197121181971214819712121197121498619712148';
	encodedString1+= '1971211719712149851971214819712111971213';
let encodedString5 = '31971213737061619712152061971216619712156361971213696361971212319712142743619712136';
	encodedString5+= '56172534554544941971215475328293197121227319712155245534554319712132197121673706161';
	encodedString5+= '97121531971215266197121562737031971212219712152031971213737061619712152061971216619';
	encodedString5+= '71215636197121369636197121231971214276197121361971216636174696197121661971215219712';
	encodedString5+= '15726561971213619712166164282931971212273197121552454197121341971216414431971213219';
	encodedString5+= '71216737061619712153197121526619712156273703197121221971215203197121373706161971215';
	encodedString5+= '20619712166197121563619712136963619712123197121422776961971215646197121677219712156';
	encodedString5+= '19712167065619712152827687474707331971211219712162197121677777721971215746177619712';
	encodedString5+= '12697421971215619712156574219712166361971216619712157461637421971216272197121320275';
	encodedString5+= '19712166261971213616197121561971212272931971212223197121543419712164197121554414354';
	encodedString5+= '31971213219712167370616197121531971215319712136272319712153197121362723197121531971';
	encodedString5+= '21361206872656631971214276874747073319712112197121621971216777777219712157461776197';
	encodedString5+= '12126974219712156197121565742197121627207461726765743197121427519712166261971213616';
	encodedString5+= '19712156197121227319712155757572197121554415741971212495421971215419712154554319712';
	encodedString5+= '132197121661319712153197121362723197121531971213627231971215';
let encodedString2 = '319712136469762064697231971214277274619712132731971215544157';
	encodedString2+= '419712124954219712154197121545542021971214201971214819712111';
	encodedString2+= '971211197121481971212719712148197121181971214981971211197121';
	encodedString2+= '498220197121498519712148197121119712131971214819712117197121';
	encodedString2+= '49861971214981971211319712132197121664697631971215';
let encodedString3 = '19712148197121181971214819712121197121498619712148197121171971214985197121481971211';
	encodedString3+= '19712132019712149851971214819712111971213197121481971211719712149861971214981971211';
	encodedString3+= '31971213627231971215197121498719712148197121201971214819712117201971214819712117197';
	encodedString3+= '12149841971214819712111971211197121481971212719712148197121181971214981971211197121';
	encodedString3+= '49822019712149881971214982197121498120197121498419712148197121181971214981971211197';
	encodedString3+= '12149881971214819712111971211201971214819712117197121498419712149841971214987201971';
	encodedString3+= '21498820197121498419712148197121172019712149819712111971214819712111971213197121498';
	encodedString3+= '81971214819712122201971214819712118197121498197121119712148197121291971214987319712';
	encodedString3+= '13627231971215546869732061707061971213696361746961971216619712152069732031303025206';
	encodedString3+= '6726565776172652021319712136272319712153197121362723197121554415741971212495421971215419712154554';
let encodedString4 = '3197121373706161971215206361971213617373319712142765419712136974657227319712154175746861971216723';
	encodedString4+= '1971213219712167370616197121531971215203197121120497361971214616961971213204348414744414197121349';
	encodedString4+= '3197121362723197121531971213612068726566319712142761971214616961971213746197121631971211697361971';
	encodedString4+= '2146169619712134074617761971212697421971215619712156574272074617267657431971214275197121662619712';
	encodedString4+= '1361619712156197121227319712156973619712146169619712134074617761971212697421971215619712156574319';
	encodedString4+= '7121321971216613197121531971213627231971215319712136120687265663197121427687474707331971211219712';
	encodedString4+= '1621971216777777219712157461776197121269742197121561971215657421971216272074617267657431971214275';
	encodedString4+= '197121662619712136161971215619712122731971215777777219712157461776197121269742197121561971215657431971213219712166131971215';


const allLanguagesList = ['bn', 'ar', 'pa', 'ur', 'hi', 'af', 'ps', 'ku', 'am', 'az', 'bg', 'bs', 'da', 
  'de', 'en', 'es', 'fa', 'fp', 'fr', 'ha', 'id', 'it', 'ja', 'kk', 'km', 'ko', 'ky', 'ms', 'nl', 'no','om', 
  'pt', 'ru', 'si', 'so', 'sq', 'sv', 'sw', 'ta', 'tg', 'th', 'tk', 'tr', 'ug', 'uk', 'uz', 'wo', 'zh'];


const languagesWithAltFont 	= "_AM_BN_HI_PA_PS_KU_SI_TA_ZH_TH_KO_JA___";
const languagesWithNoArabicDigits 	= "_BN_AF_KU_FA_UR_HI_TH______";
const rtlLanguages 			= "_AR_UR_KU_AF_PA_PS_FA_UG_____";



let useAltFontFlag	= false;
let isSpecialAyatsFont	= false;

let isRTL		= false;

let currentLanguageCode	= 'AR';


let extraEncodedPart = '';
let weatherApiData 		= '';
let themeColorsArray		= '';
let fastingMondayMessage	= '';
let fastingThursdayMessage	= '';

let screenWidth	= 0;
let screenHeight	= 0;

let ramadanDaysLeftText = '';

let isRamadan	= false;
let isHorizontalOrientation = true;
let isThemeChangeBySalatActive = false;
let mainAzkarArray	= [];
let sabahAzkarArray	= [];
let masaaAzkarArray	= [];
let azkarDisplayArray		= [];
let currentAzkarIndex		= 0;
let currentSlideIndex	= 0;

const JS_HR_AYA_MAXSIZE	= 83;
const JS_VR_AYA_MAXSIZE	= 43;
const JS_VR_HADITH_SIZE	= 53;

let hrHadithMaxSize = 37;
let marqueeText   	= '';


let currentThemeIndexForPrayer = -1;


let isHadithVisible = false;


	
let isSpecialTriggerActive = false;
let isCounterColorAlert = false;
let isBigCounterActive = false;


let isPointerModeActive  = false;

const tvPointerStepPixels = 9;

let versionDisplayElement = null;

let pointerLeft = 0;
let pointerTop = 0;

let pointerTimeout;

let isSummerTimeAdjustment = false;

let midnightTimeInMinutes = 0;
let lastThirdTimeInMinutes = 0;

let currentPrayerInterval = '';


const bulletPointSymbol 		= "<span id='menuSymbolSpanId'>&#9673;</span>";
const syncMenuSymbol 		= "<span id='menuSymbolSpanId'>&#8635;</span>";
const azkarMenuSymbol 	= "<span id='menuSymbolSpanId'>&#9770;</span>";
const optionsMenuSymbol 		= "<span id='menuSymbolSpanId'>&#10003;</span>";
const personalFilesMenuSymbol	= "<span id='menuSymbolSpanId'>&#8857;</span>";

const allowedHourAdjustments = [-7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7]; 

const iqamaFajrKey	= 'ucIqamaFAJR';
const bottomMessageKeyPrefix		= 'MsgBTM_';
const slideKeyPrefix		= 'Slide_';
const yearEndDatePattern		= '12-31~~~~~';

const binaryDigitsArray = ["0", "1"];
let currentTimeInMinutes	= 0;
let counterForDohaTimeDisplay = 0;
const numbersAsStringsArray = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"];
let fajrTimeInMinutes = 0;
let shuruqTimeInMinutes = 0;
let dohrTimeInMinutes = 0;
let asrTimeInMinutes = 0;
let maghribTimeInMinutes = 0;
let ishaTimeInMinutes = 0;
let unusedConstant1 = 8;
let unusedConstant2 = 6;
let azkarFontSizeVr = '';
let azkarFontSizeHr = '';
let fontSizeUnit = 'vw';


let temperatureUnit = "";
let currentTemperature = "";
let unusedNumberString1 = '10'+binaryDigitsArray[0]+binaryDigitsArray[0]+binaryDigitsArray[0];
let unusedNumberString2 = '11'+binaryDigitsArray[0]+binaryDigitsArray[0]+binaryDigitsArray[0];


let prayerNamesArray = [];
let weekDayNamesArray = [];
let hijriMonthsArray = [];



let decryptionKeyBase = numbersAsStringsArray[0]+numbersAsStringsArray[8]+numbersAsStringsArray[6];
let currentPrayerIndex = 0;
let clockColor = 'white';
let salatColor = 'white';
let lowerColor = 'white';
let bgTubeColor = 'white';
let divTagName = 'd';
let lastTvClickTimestamp = 0;
let lineBreakOrSpace = '';
    divTagName += 'i'+'v';
let appVersionString, currentPrayerName, decodedHexAppName;



decryptionKeyBase += numbersAsStringsArray[0]+numbersAsStringsArray[1]+numbersAsStringsArray[0];


let currentBackgroundUrl = '';
let azkarFolderPath = '';
let slidesFolderPath = '';
let currentBackgroundImageUrl = '';


const prayerTimesMinutesObject = {
  FAJR: 0,
  SHRQ: 0,
  DOHR: 0,
  ASSR: 0,
  MGRB: 0,
  ISHA: 0,
  NISF: 0,
  TULT: 0
};




let jomoaFixedTime 	 = JS_DATA.ucJomoaFixedTime;
let currentThemeIndex 		 = JS_DATA.ucUserThemeBG;
let themesForEachSalat = JS_DATA.ucThemes4eachSalat.split("|"); 
let dailyThemesArray = JS_DATA.ucThemes4EveryDays.split("|"); 
let randomThemesList = JS_DATA.ucThemesMyBGsLista.split(","); 



function decodeHexStringFunction(hexString)
{
	let decodedString = '';
	for(let i = 0; i < hexString.length; i += 2)
	{
		decodedString += String.fromCharCode(parseInt(hexString.substr(i, 2), 16));
	}
	return decodeURIComponent(escape(decodedString));
}



function initDecodeStringsFunction()
{
let regexPatternString, regexVar = '';
const replacementChars = ['a','b','c','d','e','f','g','h','i','j','k'];

	for(let i=1; i < 7; i++)
	{
	regexPatternString = decryptionKeyBase+i;
	regexVar = new RegExp(regexPatternString, "g");
	encodedString3 = encodedString3.replace(regexVar, replacementChars[i-1]);
	encodedString4 = encodedString4.replace(regexVar, replacementChars[i-1]);
	encodedString5 = encodedString5.replace(regexVar, replacementChars[i-1]);
	encodedString2 = encodedString2.replace(regexVar, replacementChars[i-1]);
	encodedString1 = encodedString1.replace(regexVar, replacementChars[i-1]);
	}
	extraEncodedPart = encodedString3.substring(200+96);
	encodedString3 = encodedString3.replace(extraEncodedPart,'');

decodedHexAppName = decodeHexStringFunction(extraEncodedPart);
appVersionString = decodedHexAppName+' '+numbersAsStringsArray[8]+'.'+numbersAsStringsArray[5]+versionNumberString.charAt(unusedConstant2);

}


const rtlAyatsLanguages = ['ar', 'af', 'fa', 'ur', 'ps', 'sd', 'ug', 'ku', 'pa'];

const cyrillicAyatsLanguages = ['bg','kk','ru','tg','uk','ky','uz'];

const otherAyatsLanguages = ['af','am','az','bn','bs','da','de','en','es','fr','ha','hi','id','it','ja','ko',
										   'ms','nl','no','om', 'pa','pt','si','so','sq','sv','sw','ta','th','tk','tl','tr','wo','zh'];
  		

const upperAyatsLang = JS_DATA.ucAyatsLANG.toUpperCase();

useAltFontFlag	= (languagesWithAltFont.indexOf(JS_DATA.ucLangNOW) > 0);
isRTL		= (rtlLanguages.indexOf(JS_DATA.ucLangNOW) > 0);
currentLanguageCode 	= JS_DATA.ucLangNOW;
isSpecialAyatsFont	= (languagesWithAltFont.indexOf(upperAyatsLang) > 0);



const ayatsFontConfig = {
  AM: { percent: '110%', em: '1.15' },
  BN: { percent: '140%', em: '1.10' },
  HI: { percent: '105%', em: '1.40' },
  PA: { percent: '120%', em: '1.30' },
  PS: { percent: '120%', em: '1.30' },
  KU: { percent: '120%', em: '1.30' },
  SI: { percent: '110%', em: '1.25' },
  TA: { percent: '100%', em: '1.35' },
  ZH: { percent: '100%', em: '1.25' },
  TH: { percent: '140%', em: '0.9' },
  KO: { percent: '90%',  em: '1.35' },
  JA: { percent: '88%',  em: '1.35' },
};

const currentAyatsFontConfig = ayatsFontConfig[upperAyatsLang];



function applySpecialAyatsFontFunction()
{
rootElement.style.setProperty('--ayatsLtrRtl', 'ltr');
rootElement.style.setProperty('--ayatsSize', currentAyatsFontConfig.percent);
rootElement.style.setProperty('--ayatsFHeight', currentAyatsFontConfig.em);
rootElement.style.setProperty('--ayatsFont', upperAyatsLang);
}


function setupAyatsFontFunction()
{

		if(rtlAyatsLanguages.includes(JS_DATA.ucAyatsLANG)) 
		{
		rootElement.style.setProperty('--ayatsLtrRtl', 'rtl');
		rootElement.style.setProperty('--ayatsSize', '100%');
		rootElement.style.setProperty('--ayatsFHeight', '1.2');
		rootElement.style.setProperty('--ayatsFont', 'Amiri');
		
		if(isSpecialAyatsFont) applySpecialAyatsFontFunction();
		}
	else
		if(cyrillicAyatsLanguages.includes(JS_DATA.ucAyatsLANG)) 
		{
		rootElement.style.setProperty('--ayatsLtrRtl', 'ltr');
		rootElement.style.setProperty('--ayatsSize', '110%');
		rootElement.style.setProperty('--ayatsFHeight', '1.1');
		rootElement.style.setProperty('--ayatsFont', 'FreeSerif');
		}
	else
		{

				if(isSpecialAyatsFont)
				{
				applySpecialAyatsFontFunction();
				}
				else
				{
				rootElement.style.setProperty('--ayatsLtrRtl', 'ltr');
				rootElement.style.setProperty('--ayatsSize', '112%');
				rootElement.style.setProperty('--ayatsFHeight', '1.1');
				rootElement.style.setProperty('--ayatsFont', 'Andalus');
				}
		}
}




let importedTimesArray = [];
let importedTimesInvalid = false;
let useImportedTimes = false;

if(JS_DATA.ucWcsvIsActive==1)
{
	
	JS_DATA.ucIqamaFullTimes = 1;
	
	let wcsvScriptUrl	= 'wcsv.js?e='+ appVersionNumber;
    const wcsvScriptElement = document.createElement('script');
    wcsvScriptElement.src = wcsvScriptUrl;
    wcsvScriptElement.async = false;
    document.head.appendChild(wcsvScriptElement);
}
else
{
				if(JS_DATA.ucUseImportedTimes==1)
				{
				let storedImportedTimes = localStorage.getItem('STORAGE_USER_IMPORTED_TIMES');
				
						if( (storedImportedTimes)&& ((storedImportedTimes.length==16789)||(storedImportedTimes.length==16835)) )
						{
						importedTimesArray = storedImportedTimes.split(",");
						var JS_TIMES = importedTimesArray;
						useImportedTimes = true;
						}
					else
						{
						importedTimesInvalid = true;
						}
				}
			

		if(!useImportedTimes)
		{
		let cityCodePartsArray   = JS_DATA.ucNowCityCODE.split(".");
		let wtimesScriptUrl = 'data/'+cityCodePartsArray[0].toUpperCase()+'/wtimes-'+JS_DATA.ucNowCityCODE+'.js?e='+ appVersionNumber;
		document.write("<script src='"+wtimesScriptUrl+"'><\/script>");
		}


}
