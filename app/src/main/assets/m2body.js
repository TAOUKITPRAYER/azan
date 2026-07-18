window.addEventListener("error", function(e) { console.log("TWKT SYS_error : ", e.message, " at ", e.filename, ":", e.lineno);});
window.addEventListener("unhandledrejection", function(e) { console.log("TWKT SYS_rejected : unhandled rejection", e.reason);});


function sleepFunction(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
} 


if(useAltFontFlag)
{
	let altFontName = currentLanguageCode;
	rootElement.style.setProperty('--csvar_backFONT', altFontName);
	rootElement.style.setProperty('--csvar_fontSECONDARY', 'Amiri');
	rootElement.style.setProperty('--csvar_mainFONT', altFontName);
	rootElement.style.setProperty('--csvar_azkarFONT',altFontName);
	rootElement.style.setProperty('--AllowVisioMainFONTS','hidden');
}
else
{
	rootElement.style.setProperty('--csvar_backFONT', 'Amiri');
	rootElement.style.setProperty('--csvar_fontSECONDARY', 'Amiri');
	rootElement.style.setProperty('--csvar_mainFONT', JS_DATA.ucScreenFont);
	rootElement.style.setProperty('--csvar_azkarFONT',JS_DATA.ucAzkarFont);
}

setupAyatsFontFunction();




function startClockIntervalFunction()
{
setInterval('clockTickFunction()', (1000)); 
}


const tvPointerElement			= document.getElementById('tvPointerImage');
const verticalMainContainer				= document.getElementById('mainVerticalContainer');
const horizontalMainContainer				= document.getElementById('mainHorizontalContainer');
const horizontalBackground				= document.getElementById('horizontalBackgroundLayer');
const verticalBackground				= document.getElementById('verticalBackgroundLayer');
const menuSections				= document.getElementById('menuSectionsContainer');
const ayaDisplayVerticalElement			= document.getElementById('ayaTextDisplayVertical');
const ayaDisplayHorizontalElement				= document.getElementById('ayaTextDisplayHorizontal');
const mosqueNameVerticalElement			= document.getElementById('mosqueNameDisplayVertical');
const mosqueNameHorizontalElement			= document.getElementById('mosqueNameDisplayHorizontal');
const dateDisplayHorizontalElement		= document.getElementById('dateDisplayHorizontal');
const hadithDisplayVerticalElement			= document.getElementById('hadithDisplayVertical');
const marqueeContainerVerticalElement			= document.getElementById('marqueeContainerVertical');
const staticMessageVerticalElement			= document.getElementById('staticMessageDisplayVertical');
const hadithDisplayHorizontalElement			= document.getElementById('hadithDisplayHorizontal');
const marqueeContainerHorizontalElement			= document.getElementById('marqueeContainerHorizontal');
const staticMessageHorizontalElement			= document.getElementById('staticMessageDisplayHorizontal');
const blackScreenVerticalElement				= document.getElementById('blackScreenVertical');
const blackScreenHorizontalElement				= document.getElementById('blackScreenHorizontal');
const fullScreenCounterVerticalElement			= document.getElementById('fullScreenCounterContainerVertical');
const fullScreenCounterHorizontalElement			= document.getElementById('fullScreenCounterContainerHorizontal');
const fullScreenBgHorizontal		= document.getElementById('fullScreenCounterBackgroundHorizontal');
const fullScreenTimeVertical			= document.getElementById('fullScreenCounterTimeDisplayVertical');
const fullScreenLabelVertical			= document.getElementById('fullScreenCounterLabelVertical');
const fullScreenLabelHorizontal			= document.getElementById('fullScreenCounterLabelHorizontal');
const fullScreenTimeHorizontal			= document.getElementById('fullScreenCounterTimeDisplayHorizontal');
const fullScreenMiniClockHorizontal			= document.getElementById('fullScreenCounterMiniClockHorizontal');
const secondCounterVerticalElement			= document.getElementById('secondCounterContainerVertical');
const secondCounterHorizontalElement			= document.getElementById('secondCounterContainerHorizontal');
const prayerRowFajrVerticalElement				= document.getElementById('prayerRowFajrVertical');
const prayerRowShrqVerticalElement				= document.getElementById('prayerRowShrqVertical');
const prayerRowDohrVerticalElement				= document.getElementById('prayerRowDohrVertical');
const prayerRowAsrVerticalElement				= document.getElementById('prayerRowAsrVertical');
const prayerRowMgrbVerticalElement				= document.getElementById('prayerRowMgrbVertical');
const prayerRowIshaVerticalElement				= document.getElementById('prayerRowIshaVertical');
const prayerCellFajrHorizontalElement				= document.getElementById('prayerCellFajrHorizontal');
const prayerCellShrqHorizontalElement				= document.getElementById('prayerCellShrqHorizontal');
const prayerCellDohrHorizontalElement				= document.getElementById('prayerCellDohrHorizontal');
const prayerCellAsrHorizontalElement				= document.getElementById('prayerCellAsrHorizontal');
const prayerCellMgrbHorizontalElement				= document.getElementById('prayerCellMgrbHorizontal');
const prayerCellIshaHorizontalElement				= document.getElementById('prayerCellIshaHorizontal');
const azanPopupVerticalElement			= document.getElementById('azanPopupVertical');
const azanPopupBgVertical		= document.getElementById('azanPopupBackgroundVertical');
const azanPopupHorizontalElement			= document.getElementById('azanPopupHorizontal');
const azanPopupBgHorizontal		= document.getElementById('azanPopupBackgroundHorizontal');
const iqamaPopupVerticalElement			= document.getElementById('iqamaPopupVertical');
const iqamaPopupHorizontalElement			= document.getElementById('iqamaPopupHorizontal');
const iqamaPopupBgVertical		= document.getElementById('iqamaPopupBackgroundVertical');
const iqamaPopupBgHorizontal		= document.getElementById('iqamaPopupBackgroundHorizontal');
const dohaPopupVerticalElement			= document.getElementById('dohaPopupVertical');
const dohaPopupHorizontalElement			= document.getElementById('dohaPopupHorizontal');
const dohaPopupBgVertical		= document.getElementById('dohaPopupBackgroundVertical');
const dohaPopupBgHorizontal		= document.getElementById('dohaPopupBackgroundHorizontal');
const azkarContainerVerticalElement				= document.getElementById('azkarContainerVertical');
const azkarContainerHorizontalElement				= document.getElementById('azkarContainerHorizontal');
const slidesContainerVerticalElement				= document.getElementById('slidesContainerVertical');
const slidesContainerHorizontalElement				= document.getElementById('slidesContainerHorizontal');
const noMobileImageVerticalElement			= document.getElementById('noMobileImageVertical');
const noMobileImageHorizontalElement			= document.getElementById('noMobileImageHorizontal');
const prayerTimesContainerHorizontalElement			= document.getElementById('prayerTimesContainerHorizontal');
const prayerTimesTableHorizontalElement			= document.getElementById('prayerTimesTableHorizontal');
const iqamaCounterCircleHorizontalElement				= document.getElementById('iqamaCounterCircleHorizontal');
const iqamaCounterCircleVerticalElement				= document.getElementById('iqamaCounterCircleVertical');
const azanVoiceCheckboxElement			= document.getElementById('azanIqamaByVoiceCheckbox');
const shortAzanCheckboxElement			= document.getElementById('shortAzanCheckbox');
const shortIqamaCheckboxElement			= document.getElementById('shortIqamaCheckbox');
const ramadanIshaCheckboxElement		= document.getElementById('ramadanIsha30minCheckbox');
const summerTimeCheckboxElement			= document.getElementById('summerTimeCheckbox');
const forceOneHourMoreCheckboxElement		= document.getElementById('forceOneHourMoreCheckbox');
const forceOneHourLessCheckboxElement		= document.getElementById('forceOneHourLessCheckbox');
const use24HoursCheckboxElement				= document.getElementById('use24HoursCheckbox');
const meteoActiveCheckboxElement			= document.getElementById('meteoActiveCheckbox');
const fullClockCheckboxElement		= document.getElementById('fullClockCheckbox');
const useWcsvCheckboxElement			= document.getElementById('useWcsvCheckbox');
const arabicDigitsCheckboxElement		= document.getElementById('arabicDigitsCheckbox');
const fullIqamaTimesCheckboxElement		= document.getElementById('fullIqamaTimesCheckbox');
const iqamaCounterCheckboxElement		= document.getElementById('iqamaCounterCheckbox');
const blackScreenInPrayingCheckboxElement			= document.getElementById('blackScreenInPrayingCheckbox');
const slidesActiveCheckboxElement			= document.getElementById('slidesActiveCheckbox');
const remindersActiveCheckboxElement		= document.getElementById('remindersActiveCheckbox');
const ramadanDaysLeftCheckboxElement	= document.getElementById('ramadanDaysLeftCheckbox');
const slidesRandomCheckboxElement		= document.getElementById('slidesRandomCheckbox');
const azkarActiveCheckboxElement			= document.getElementById('azkarActiveCheckbox');
const repeatAzkarCheckboxElement	= document.getElementById('repeatAzkarOnceCheckbox');
const azkar5minPictureCheckboxElement		= document.getElementById('azkar5minPictureCheckbox');
const messageDisplayVerticalElement			= document.getElementById('messageTextDisplayVertical');
const messageDisplayHorizontalElement				= document.getElementById('messageTextDisplayHorizontal');
const mainMenuElement				= document.getElementById('mainMenuContainer');
const ayaContainerVertical			= document.getElementById('ayaDisplayContainerHorizontal');
const messageContainerVertical			= document.getElementById('messageDisplayContainerHorizontal');
const menuContentElement				= document.getElementById('menuContentContainer');
const ayaContainerHorizontal			= document.getElementById('ayaDisplayContainerVertical');

const prayerTimesContainerVerticalElement		= document.getElementById('prayerTimesContainerVertical');

const fullClockTimeHorizontalElement			= document.getElementById('fullClockTimeDisplayHorizontal');
const fullClockTimeVerticalElement			= document.getElementById('fullClockTimeDisplayVertical');

const miniClockTimeHorizontalElement			= document.getElementById('miniClockTimeDisplayHorizontal');
const miniClockTimeVerticalElement			= document.getElementById('miniClockTimeDisplayVertical');

const weatherWidgetHorizontalElement			= document.getElementById('weatherWidgetHorizontal');
const appLogoVerticalElement			= document.getElementById('appLogoImage');

const azkarClockVerticalElement				= document.getElementById('azkarClockDisplayVertical');
const azkarClockHorizontalElement				= document.getElementById('azkarClockDisplayHorizontal');
const slidesClockVerticalElement				= document.getElementById('slidesClockDisplayVertical');
const slidesClockHorizontalElement				= document.getElementById('slidesClockDisplayHorizontal');
const picture5minClockVerticalElement				= document.getElementById('picture5minClockDisplayVertical');
const picture5minClockHorizontalElement				= document.getElementById('picture5minClockDisplayHorizontal');

const inputDialogElement			= document.getElementById('inputDialogContainer');
const inputDialogMessageElement			= document.getElementById('inputDialogTitle');
const inputDialogInputElement			= document.getElementById('inputDialogTextField');


const countryListContainerElement		= document.getElementById('countryListContent');
const cityListContainerElement		= document.getElementById('cityListContent');

const countryButtonElement		= document.getElementById('selectCountryButton');
const cityButtonElement		= document.getElementById('selectCityButton');

const prayerNameFajrVerticalSpanElement					= document.getElementById('prayerNameFajrVertical');
const prayerNameShrqVerticalSpanElement					= document.getElementById('prayerNameShrqVertical');
const prayerNameDohrVerticalSpanElement					= document.getElementById('prayerNameDohrVertical');
const prayerNameAsrVerticalSpanElement					= document.getElementById('prayerNameAsrVertical');
const prayerNameMgrbVerticalSpanElement					= document.getElementById('prayerNameMgrbVertical');
const prayerNameIshaVerticalSpanElement					= document.getElementById('prayerNameIshaVertical');

const azanTimeFajrVerticalSpanElement					= document.getElementById('azanTimeFajrVertical');
const azanTimeShrqVerticalSpanElement					= document.getElementById('azanTimeShrqVertical');
const azanTimeDohrVerticalSpanElement					= document.getElementById('azanTimeDohrVertical');
const azanTimeAsrVerticalSpanElement					= document.getElementById('azanTimeAsrVertical');
const azanTimeMgrbVerticalSpanElement					= document.getElementById('azanTimeMgrbVertical');
const azanTimeIshaVerticalSpanElement					= document.getElementById('azanTimeIshaVertical');



const prayerNameFajrHorizontalSpanElement				= document.getElementById('prayerNameFajrHorizontal');
const prayerNameShrqHorizontalSpanElement				= document.getElementById('prayerNameShrqHorizontal');
const prayerNameDohrHorizontalSpanElement				= document.getElementById('prayerNameDohrHorizontal');
const prayerNameAsrHorizontalSpanElement				= document.getElementById('prayerNameAsrHorizontal');
const prayerNameMgrbHorizontalSpanElement				= document.getElementById('prayerNameMgrbHorizontal');
const prayerNameIshaHorizontalSpanElement				= document.getElementById('prayerNameIshaHorizontal');

const azanTimeFajrHorizontalSpanElement				= document.getElementById('azanTimeFajrHorizontal');
const azanTimeShrqHorizontalSpanElement				= document.getElementById('azanTimeShrqHorizontal');
const azanTimeDohrHorizontalSpanElement				= document.getElementById('azanTimeDohrHorizontal');
const azanTimeAsrHorizontalSpanElement				= document.getElementById('azanTimeAsrHorizontal');
const azanTimeMgrbHorizontalSpanElement				= document.getElementById('azanTimeMgrbHorizontal');
const azanTimeIshaHorizontalSpanElement				= document.getElementById('azanTimeIshaHorizontal');


const fileInputElementHidden		= document.getElementById('fileInputElement');


const prayerNameFajrMiniHorizontalElement				= document.getElementById('prayerNameFajrMiniHorizontal');
const prayerNameShrqMiniHorizontalElement				= document.getElementById('prayerNameShrqMiniHorizontal');
const prayerNameDohrMiniHorizontalElement				= document.getElementById('prayerNameDohrMiniHorizontal');
const prayerNameAsrMiniHorizontalElement				= document.getElementById('prayerNameAsrMiniHorizontal');
const prayerNameMgrbMiniHorizontalElement				= document.getElementById('prayerNameMgrbMiniHorizontal');
const prayerNameIshaMiniHorizontalElement				= document.getElementById('prayerNameIshaMiniHorizontal');

const prayerTimeFajrMiniHorizontalSpanElement				= document.getElementById('prayerTimeFajrMiniHorizontal');
const prayerTimeShrqMiniHorizontalSpanElement				= document.getElementById('prayerTimeShrqMiniHorizontal');
const prayerTimeDohrMiniHorizontalSpanElement				= document.getElementById('prayerTimeDohrMiniHorizontal');
const prayerTimeAsrMiniHorizontalSpanElement				= document.getElementById('prayerTimeAsrMiniHorizontal');
const prayerTimeMgrbMiniHorizontalSpanElement				= document.getElementById('prayerTimeMgrbMiniHorizontal');
const prayerTimeIshaMiniHorizontalSpanElement				= document.getElementById('prayerTimeIshaMiniHorizontal');


const audioAzanMainElement				= document.getElementById('audioAzanElement');
const audioFajrAzanElement				= document.getElementById('audioFajrElement');
const audioShortAzanElementVar		= document.getElementById('audioShortAzanElement');
const audioShortIqamaElementVar		= document.getElementById('audioShortIqamaElement');
const audioBeepElementVar			= document.getElementById('audioBeepElement');
const audioTeetElementVar				= document.getElementById('audioTeetElement');
const audioAlert1ElementVar			= document.getElementById('audioAlert1Element');
const audioDrop1ElementVar			= document.getElementById('audioDrop1Element');
const audioDrop3ElementVar			= document.getElementById('audioDrop3Element');
const audioAlertsElementVar			= document.getElementById('audioAlertsElement');


const prayerNameFajrMiniVerticalElement				= document.getElementById('prayerNameFajrMiniVertical');
const prayerNameShrqMiniVerticalElement				= document.getElementById('prayerNameShrqMiniVertical');
const prayerNameDohrMiniVerticalElement				= document.getElementById('prayerNameDohrMiniVertical');
const prayerNameAsrMiniVerticalElement				= document.getElementById('prayerNameAsrMiniVertical');
const prayerNameMgrbMiniVerticalElement				= document.getElementById('prayerNameMgrbMiniVertical');
const prayerNameIshaMiniVerticalElement				= document.getElementById('prayerNameIshaMiniVertical');

const prayerTimeFajrMiniVerticalSpanElement				= document.getElementById('prayerTimeFajrMiniVertical');
const prayerTimeShrqMiniVerticalSpanElement				= document.getElementById('prayerTimeShrqMiniVertical');
const prayerTimeDohrMiniVerticalSpanElement				= document.getElementById('prayerTimeDohrMiniVertical');
const prayerTimeAsrMiniVerticalSpanElement				= document.getElementById('prayerTimeAsrMiniVertical');
const prayerTimeMgrbMiniVerticalSpanElement				= document.getElementById('prayerTimeMgrbMiniVertical');
const prayerTimeIshaMiniVerticalSpanElement				= document.getElementById('prayerTimeIshaMiniVertical');

const bodyElement					= document.getElementById('bodyElementId');


const iqamaTimeFajrVerticalSpanElement					= document.getElementById('iqamaTimeFajrVertical');
const iqamaTimeShrqVerticalSpanElement					= document.getElementById('iqamaTimeShrqVertical');
const iqamaTimeDohrVerticalSpanElement					= document.getElementById('iqamaTimeDohrVertical');
const iqamaTimeAsrVerticalSpanElement					= document.getElementById('iqamaTimeAsrVertical');
const iqamaTimeMgrbVerticalSpanElement					= document.getElementById('iqamaTimeMgrbVertical');
const iqamaTimeIshaVerticalSpanElement					= document.getElementById('iqamaTimeIshaVertical');

const iqamaTimeFajrHorizontalSpanElement					= document.getElementById('iqamaTimeFajrHorizontal');
const iqamaTimeShrqHorizontalSpanElement					= document.getElementById('iqamaTimeShrqHorizontal');
const iqamaTimeDohrHorizontalSpanElement					= document.getElementById('iqamaTimeDohrHorizontal');
const iqamaTimeAsrHorizontalSpanElement					= document.getElementById('iqamaTimeAsrHorizontal');
const iqamaTimeMgrbHorizontalSpanElement					= document.getElementById('iqamaTimeMgrbHorizontal');
const iqamaTimeIshaHorizontalSpanElement					= document.getElementById('iqamaTimeIshaHorizontal');

const tawkitNameHorizontalElement				= document.getElementById('tawkitNameDisplayHorizontal');
const cityCodeHorizontalElement				= document.getElementById('cityCodeDisplayHorizontal');
const aboutPopupElement				= document.getElementById('aboutPopupId');

const aboutAppTitleElement	= document.getElementById('aboutAppTitle');

const weatherTempVerticalElement			= document.getElementById('weatherTempVertical');
const weatherIconVerticalElement			= document.getElementById('weatherIconImageVertical');
const weatherIconHorizontalElement			= document.getElementById('weatherIconImageHorizontal');
const weatherTempHorizontalElement			= document.getElementById('weatherTempHorizontal');

const weatherMinMaxHorizontalElement				= document.getElementById('weatherMinMaxSpanHorizontal');
const weatherMinMaxVerticalElement				= document.getElementById('weatherMinMaxSpanVertical');

const mobileAlertButtonElement			= document.getElementById('closeMobileAlertButton');

const internetStatusHorizontalElement				= document.getElementById('internetStatusIndicatorHorizontal');
const internetStatusVerticalElement				= document.getElementById('internetStatusIndicatorVertical');
const dateDisplayVerticalElement			= document.getElementById('dateDisplayVertical');
const cityCodeVerticalElement			= document.getElementById('tawkitVersionDisplay');

const aboutDescriptionElement			= document.getElementById('aboutDescription');
const aboutExtraInfoElement		= document.getElementById('aboutExtraInfo');
const creditsContentElement		= document.getElementById('creditsContent');

const locationLoadingIndicator				= document.getElementById('locationSectionTitle');

const slidesTableBody 			= document.getElementById('slidesTable');
const messagesTableBody 				= document.getElementById('bottomMessagesTable');
const remindersTableBody		= document.getElementById('remindersTable');

const currentThemeDisplayElement				= document.getElementById('currentThemeDisplay');
const appLogoHorizontalElement			= document.getElementById('appLogoImageHorizontal');
const jomoaCounterHorizontalElement			= document.getElementById('jomoaCounterHorizontal');
const nextPrayerCounterHorizontalElement			= document.getElementById('nextPrayerCounterHorizontal');
const iqamaCounterTimeVerticalElement				= document.getElementById('iqamaCounterTimeDisplayHorizontal');
const iqamaCounterTimeHorizontalElement				= document.getElementById('iqamaCounterTimeDisplayVertical');
const unusedElementVar			= document.getElementById('unusedElement');

const gpsPositionDisplayElement				= document.getElementById('gpsPositionDisplay');
const dohaTimeHorizontalElement			= document.getElementById('dohaTimeDisplayHorizontal');
const dohaTimeVerticalElement			= document.getElementById('dohaTimeDisplayVertical');
const dateTextSpanHorizontalElement	= document.getElementById('dateTextSpanHorizontal');


const nextPrayerLabelHorizontalElement		= document.getElementById('nextPrayerLabelHorizontal');
const nextPrayerLabelVerticalElement		= document.getElementById('bigNextPrayCounterToggleDiv');
const jomoaLabelHorizontalElement			= document.getElementById('jomoaLabelHorizontal');
const secondJomoaDisplayHorizontal		= document.getElementById('secondJomoaTimeDisplayHorizontal');

const azanPopupTitleVerticalElement			= document.getElementById('azanPopupTitleVertical');
const azanPopupTitleHorizontalElement			= document.getElementById('azanPopupTitleHorizontal');

const iqamaCounterLabelVerticalElement				= document.getElementById('iqamaCounterLabelVertical');
const iqamaCounterLabelHorizontalElement				= document.getElementById('iqamaCounterLabelHorizontal');

const prayerNameFajrAdjustElement				= document.getElementById('prayerNameFajrAdjust');
const prayerNameShrqAdjustElement				= document.getElementById('prayerNameShrqAdjust');
const prayerNameDohrAdjustElement				= document.getElementById('prayerNameDohrAdjust');
const prayerNameAsrAdjustElement				= document.getElementById('prayerNameAsrAdjust');
const prayerNameMgrbAdjustElement			= document.getElementById('prayerNameMgrbAdjust');
const prayerNameIshaAdjustElement				= document.getElementById('prayerNameIshaAdjust');

const themeSaturdayCellElement					= document.getElementById('jw6');

const themeFajrCellElement					= document.getElementById('jc0');
const themeShrqCellElement					= document.getElementById('jc1');
const themeDohrCellElement					= document.getElementById('jc2');
const themeAsrCellElement					= document.getElementById('jc3');
const themeMgrbCellElement					= document.getElementById('jc4');
const themeIshaCellElement					= document.getElementById('jc5');

const themeSundayCellElement					= document.getElementById('jw0');
const themeMondayCellElement					= document.getElementById('jw1');
const themeTuesdayCellElement					= document.getElementById('jw2');
const themeWednesdayCellElement					= document.getElementById('jw3');
const themeThursdayCellElement					= document.getElementById('jw4');
const themeFridayCellElement					= document.getElementById('jw5');

const fullScreenButtonElement		= document.getElementById('fullScreenButton');

const menuVersionDisplayElement			= document.getElementById('menuVersionDisplay'); 

const durationFajrValueElement	= document.getElementById('durationFajrValue');
const durationDohrValueElement	= document.getElementById('durationDohrValue');
const durationAsrValueElement		= document.getElementById('durationAsrValue');
const durationMgrbValueElement	= document.getElementById('durationMgrbValue');
const durationIshaValueElement	= document.getElementById('durationIshaValue');


const dimmerBeforeValueElement		= document.getElementById('dimmerBeforeValue');
const dimmerAfterValueElement		= document.getElementById('dimmerAfterValue');

const alertToastElement		= document.getElementById('alertToastDiv');


const autoStartContainerElement			= document.getElementById('autoStartContainer');
const autoStartCheckboxElement		= document.getElementById('autoStartCheckbox');
const autoStartLabelElement		= document.getElementById('autoStartLabel');




const athanFajrValueElement = document.getElementById('athanMinutesFajrValue');
const athanShrqValueElement = document.getElementById('athanMinutesShrqValue');
const athanDohrValueElement = document.getElementById('athanMinutesDohrValue');
const athanAsrValueElement = document.getElementById('athanMinutesAsrValue');
const athanMgrbValueElement = document.getElementById('athanMinutesMgrbValue');
const athanIshaValueElement = document.getElementById('athanMinutesIshaValue');




const nightPrayersContainerVerticalElement 	= document.getElementById('nightPrayersContainerVertical');
const midnightTimeVerticalElement 	= document.getElementById('midnightTimeDisplayVertical');
const lastThirdTimeVerticalElement 	= document.getElementById('lastThirdTimeDisplayVertical');
const tomorrowFajrTimeVerticalElement 	= document.getElementById('tomorrowFajrTimeDisplayVertical');

const nightPrayersContainerHorizontalElement 	= document.getElementById('nightPrayersContainerHorizontal');
const midnightTimeHorizontalElement 	= document.getElementById('midnightTimeDisplayHorizontal');
const lastThirdTimeHorizontalElement 	= document.getElementById('lastThirdTimeDisplayHorizontal');
const tomorrowFajrTimeHorizontalElement 	= document.getElementById('tomorrowFajrTimeDisplayHorizontal');




mosqueNameVerticalElement.innerHTML 		= JS_DATA.ucMosqueName;
mosqueNameHorizontalElement.innerHTML 		= JS_DATA.ucMosqueName;



azanVoiceCheckboxElement.checked		= (JS_DATA.ucAzanIqamaByVoice == 1);
shortAzanCheckboxElement.checked			= (JS_DATA.ucShortAzanActive == 1);
shortIqamaCheckboxElement.checked		= (JS_DATA.ucShortIqamaActive == 1);
ramadanIshaCheckboxElement.checked	= (JS_DATA.ucRamadanDoIsha30min == 1);
summerTimeCheckboxElement.checked		= (JS_DATA.ucInSummerAdd1Hour == 1);

forceOneHourMoreCheckboxElement.checked		= (JS_DATA.ucForce1HourMore == 1);
forceOneHourLessCheckboxElement.checked		= (JS_DATA.ucForce1HourLess == 1);
use24HoursCheckboxElement.checked			= (JS_DATA.ucActivate24Hours == 1);

useWcsvCheckboxElement.checked			= (JS_DATA.ucWcsvIsActive == 1);
fullIqamaTimesCheckboxElement.checked	= (JS_DATA.ucIqamaFullTimes == 1);
iqamaCounterCheckboxElement.checked	= (JS_DATA.ucIqamaCounter == 1);
blackScreenInPrayingCheckboxElement.checked			= (JS_DATA.ucBlackScreenInPraying == 1);
slidesActiveCheckboxElement.checked		= (JS_DATA.ucSlidesActive == 1);
slidesRandomCheckboxElement.checked		= (JS_DATA.ucSlidesRandom == 1);
azkarActiveCheckboxElement.checked		= (JS_DATA.ucAzkarActive == 1);
repeatAzkarCheckboxElement.checked	= (JS_DATA.ucRepeatMainAzkarOnce > 0);
remindersActiveCheckboxElement.checked		= (JS_DATA.ucRemindersActive == 1);
ramadanDaysLeftCheckboxElement.checked	= (JS_DATA.ucRamadanDaysLeft== 1);






let decodedStringVar = prayerNameFajrAdjustElement.id+prayerNameShrqAdjustElement.id+prayerNameDohrAdjustElement.id;
    decodedStringVar+= prayerNameAsrAdjustElement.id+prayerNameMgrbAdjustElement.id+prayerNameIshaAdjustElement.id;
    decodedStringVar = decodedStringVar.replace(/jc/g,'');



function setFontRadioButtonsFunction()
{
	try
	{
		document.getElementById('APPUI-' + JS_DATA.ucScreenFont).checked = true;
		document.getElementById('TIMES-' + JS_DATA.ucTimesFont).checked = true;
		document.getElementById('CLOCK-' + JS_DATA.ucClockFont).checked = true;
		document.getElementById('AZKAR-' + JS_DATA.ucAzkarFont).checked = true;
	}
	catch(err)
	{}
}


function doAlert(str)
{
    alertToastElement.textContent = str;
    alertToastElement.style.display = 'flex';
    setTimeout(function() { alertToastElement.style.display = 'none'; }, 12000);
}



function applyThemeFunction(isThemeChangeWithTransition)
{

let themeColorsString = '';
let isVrThemeSpecial = false;
let isLightTheme = false;

const specialVrThemesList = [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,18,19,34,35,38,39];
const specialThemeIndices = [2,3,4,8,9];
const lightThemeIndices = [38];
const otherSpecialThemes = [15,16,21];	


themeColorsString = JS_COLORS_FOR_DARK_THEMES;


	if(isHorizontalOrientation)
	{
	
		if( (JS_DATA.ucUserFilesACTIVE == 1) && (userUploadedImages[0]) )
		currentBackgroundUrl = userUploadedImages[0];
		else
		currentBackgroundUrl = 'themes/HR-' + currentThemeIndex + '.jpg';
	

		azkarFolderPath = 'azkar/hr';
		slidesFolderPath = 'slides/hr';



	applyHrLayoutFunction();
	}
	else
	{
		
		if( (JS_DATA.ucUserFilesACTIVE == 1) && (userUploadedImages[0]) )
		currentBackgroundUrl = userUploadedImages[0]; 
		else		
		currentBackgroundUrl = 'themes/VR-' + currentThemeIndex + '.jpg';

		azkarFolderPath = 'azkar/vr';
		slidesFolderPath = 'slides/vr';


		if(lightThemeIndices.indexOf(currentThemeIndex) !== -1) themeColorsString = JS_COLORS_FOR_LIGHT_THEMES;


		if(specialVrThemesList.indexOf(currentThemeIndex) !== -1)
		{
			lineBreakOrSpace = "<br>";
			verticalMainContainer.className = 'classicVrClass'; 
		}
		else if(otherSpecialThemes.indexOf(currentThemeIndex) !== -1)
		{
			lineBreakOrSpace = " ";
			verticalMainContainer.className = 'specialVrClass';
		}
		else
		{
			lineBreakOrSpace = " ";
			verticalMainContainer.className = '';
			rootElement.style.setProperty('--varShrq', 'block');
		}

		dateDisplayVerticalElement.innerHTML = currentDayName + lineBreakOrSpace + cityCodeDisplayString;
		if(specialThemeIndices.indexOf(currentThemeIndex) !== -1) isVrThemeSpecial = true;
		if(lightThemeIndices.indexOf(currentThemeIndex) !== -1) isLightTheme = true;
	}




	if(isThemeChangeWithTransition)
	{
	horizontalBackground.style.background = currentBackgroundImageUrl;
	horizontalBackground.style.backgroundRepeat = 'no-repeat';
	horizontalBackground.style.backgroundSize = '100% 100%';
    horizontalBackground.className = 'backgroundFadeInClass';
	
	verticalBackground.style.background = currentBackgroundImageUrl;
	verticalBackground.style.backgroundRepeat = 'no-repeat';
	verticalBackground.style.backgroundSize = '100% 100%';
    verticalBackground.className = 'backgroundFadeInClass';
    
	setTimeout(clearBackgroundTransitionFunction, 3000);
	}



	if(isHorizontalOrientation)
	{
	horizontalMainContainer.style.background = 'url(' + currentBackgroundUrl + ')';
	horizontalMainContainer.style.backgroundRepeat = 'no-repeat';
	horizontalMainContainer.style.backgroundSize = '100% 100%';
	
	azanPopupBgHorizontal.style.background = 'url(' + currentBackgroundUrl + ')';
	azanPopupBgHorizontal.style.backgroundRepeat = 'no-repeat';
	azanPopupBgHorizontal.style.backgroundSize = '100% 100%';
			fullScreenBgHorizontal.style.background = 'url(' + currentBackgroundUrl + ')';
			fullScreenBgHorizontal.style.backgroundRepeat = 'no-repeat';
			fullScreenBgHorizontal.style.backgroundSize = '100% 100%';
	iqamaPopupBgHorizontal.style.background = 'url(' + currentBackgroundUrl + ')';
	iqamaPopupBgHorizontal.style.backgroundRepeat = 'no-repeat';
	iqamaPopupBgHorizontal.style.backgroundSize = '100% 100%';
	
	dohaPopupBgHorizontal.style.background = 'url(' + currentBackgroundUrl + ')';
	dohaPopupBgHorizontal.style.backgroundRepeat = 'no-repeat';
	dohaPopupBgHorizontal.style.backgroundSize = '100% 100%';

	}
else
	{
	verticalMainContainer.style.background = 'url(' + currentBackgroundUrl + ')';
	verticalMainContainer.style.backgroundRepeat = 'no-repeat';
	verticalMainContainer.style.backgroundSize = '100% 100%';
	
	azanPopupBgVertical.style.background = 'url(' + currentBackgroundUrl + ')';
	azanPopupBgVertical.style.backgroundRepeat = 'no-repeat';
	azanPopupBgVertical.style.backgroundSize = '100% 100%';
	
	iqamaPopupBgVertical.style.background = 'url(' + currentBackgroundUrl + ')';
	iqamaPopupBgVertical.style.backgroundRepeat = 'no-repeat';
	iqamaPopupBgVertical.style.backgroundSize = '100% 100%';
	
	dohaPopupBgVertical.style.background = 'url(' + currentBackgroundUrl + ')';
	dohaPopupBgVertical.style.backgroundRepeat = 'no-repeat';
	dohaPopupBgVertical.style.backgroundSize = '100% 100%';
	}







	themeColorsArray = themeColorsString.split('|');

	clockColor = themeColorsArray[0];
	salatColor = themeColorsArray[1];
	lowerColor = themeColorsArray[2];
	bgTubeColor = themeColorsArray[3];

	rootElement.style.setProperty('--esCLOCK', clockColor);
	rootElement.style.setProperty('--esSALATS', salatColor);
	rootElement.style.setProperty('--esLOWER', lowerColor);
	rootElement.style.setProperty('--esBGTUB', bgTubeColor);

	if(isVrThemeSpecial)
	{
		rootElement.style.setProperty('--vrDYNc1', bgTubeColor);
	}
	else
	{
		rootElement.style.setProperty('--vrDYNc1', salatColor);
	}

	if(isLightTheme)
	{
		rootElement.style.setProperty('--esGOLDEN', '#994112');
		rootElement.style.setProperty('--esCountr', '#E8E1DA');
	}
	else
	{
		rootElement.style.setProperty('--esGOLDEN', '#F3BB7C');
		rootElement.style.setProperty('--esCountr', '#000000');
	}

	updateNextPrayerDisplayFunction(currentTimeInMinutes);
	displayRandomAyaFunction();
	updateMosqueAndDateDisplayFunction();
	updateBigNextPrayCounterFunction();

	currentThemeDisplayElement.style.background = 'url(' + currentBackgroundUrl + ')';
	currentThemeDisplayElement.style.backgroundRepeat = 'no-repeat';
	currentThemeDisplayElement.style.backgroundSize = 'auto 100%';
	currentThemeDisplayElement.innerHTML = currentThemeIndex;

	currentBackgroundImageUrl = 'url(' + currentBackgroundUrl + ')';


}


function setThemeFunction(themeIndexParam, withTransitionParam)
{
	currentThemeIndex = themeIndexParam;
	JS_DATA.ucUserThemeBG = currentThemeIndex;
	saveSettingsToStorageFunction();
	applyThemeFunction(withTransitionParam);
}



function clearBackgroundTransitionFunction()
{
	horizontalBackground.style.background = 'none';
	verticalBackground.style.background = 'none';
	horizontalBackground.className = 'backgroundFadeOutClass';
	verticalBackground.className = 'backgroundFadeOutClass';
}










function addAboutMenuItemFunction()
{
		function showAboutPopupWrapper(){ closeMenuFunction(); showElementFunction('aboutPopupId'); }
		
		if(currentLanguageCode == 'AR') JS_eLang.cx_OPTION_9 = decodeHexStringFunction(encodedString1);
		const aboutMenuItemElement = document.createElement(divTagName);
		aboutMenuItemElement.innerHTML = bulletPointSymbol + JS_eLang.cx_OPTION_9;
		aboutMenuItemElement.onclick = showAboutPopupWrapper;
		menuSections.appendChild(aboutMenuItemElement);
		
		versionDisplayElement = document.createElement(divTagName);
		versionDisplayElement.id = 'versionDisplayId';
		verticalMainContainer.appendChild(versionDisplayElement);
}



function incrementThemeFunction()
{
	if( (JS_DATA.ucUserFilesACTIVE == 1) && (userUploadedImages[0]) ) return;
	
	currentThemeIndex++;
	if(currentThemeIndex > 39) currentThemeIndex = 0;
	setThemeFunction(currentThemeIndex, false);
}




function decrementThemeFunction()
{
	if( (JS_DATA.ucUserFilesACTIVE == 1) && (userUploadedImages[0]) ) return;
	
	currentThemeIndex--;
	if(currentThemeIndex < 0) currentThemeIndex = 39;
	setThemeFunction(currentThemeIndex, false);
}



function setUploadInfoMessageFunction(uploadMessage)
{
document.getElementById('uploadInfoText').innerHTML = uploadMessage;
}




let userUploadedImages = [null, null]; 
const storageKeysArray = ['img_THEME', 'img_AZKAR'];




					

function saveImageToStorageFunction(imageFile, storageKey, imageIndex)
{
	return new Promise((resolve, reject) =>
	{
		
		const fileReader = new FileReader();
		
		fileReader.onload = function(event)
		{
				try
				{
					const imageDataUrl = event.target.result;
					localStorage.setItem(storageKey, imageDataUrl);
					userUploadedImages[imageIndex] = imageDataUrl;
					resolve(storageKey);
				}
				catch (error)
				{
					if (error.name === 'QuotaExceededError')
					{
						setUploadInfoMessageFunction('ERROR: Storage quota exceeded! Use image size should be less than 1MB max.');
						updateStorageInfoFunction();
					}
					else
					{
						setUploadInfoMessageFunction('ERROR saving image:', error);
					}
				}
			};
			
			fileReader.onerror = function(error)
			{
				setUploadInfoMessageFunction('ERROR reading file:', error);
			};
		
	fileReader.readAsDataURL(imageFile);
	});
}



function handleImageUploadFunction(event, imageStorageKey, iniNbx)
{

	const selectedFile = event.target.files[0];
	
	if (!selectedFile)
	{
		setUploadInfoMessageFunction('error : No file selected');
		return;
	}
	
	if (!selectedFile.type.startsWith('image/'))
	{
		setUploadInfoMessageFunction('eror : File is not an image');
		return;
	}
	
	
	
	saveImageToStorageFunction(selectedFile, imageStorageKey, iniNbx)
		.then(() =>
		{
			document.getElementById('cjpf'+iniNbx).src = userUploadedImages[iniNbx];
			applyThemeFunction(true);
		})
		.catch((error) =>
		{
			setUploadInfoMessageFunction('Error:', error);
		});
}




	



function uploadImageFunction(nbrx)
{

			fileInputElementHidden.addEventListener('change', function(event)
			{
			handleImageUploadFunction(event, storageKeysArray[nbrx], nbrx);
			}, {once: true});
			
			fileInputElementHidden.click();
}


function removeUploadedImageFunction(nbrx)
{
	localStorage.removeItem( storageKeysArray[nbrx]);
	userUploadedImages[nbrx]	= null;
	updateStorageInfoFunction();
	document.getElementById('cjpf'+nbrx).src = '';

	if(nbrx == 0) applyThemeFunction(true);
}




function updateFixedTimesDisplayFunction()
{

const jomoaFixedTimeButtonElement = document.getElementById('jomoaFixedTimeButton');
const fixedIqamaFajrButtonElement = document.getElementById('fixedIqamaFajrButton')
const fixedIqamaDohrButtonElement = document.getElementById('fixedIqamaDohrButton');
const fixedIqamaAsrButtonElement = document.getElementById('fixedIqamaAsrButton');
const fixedIqamaIshaButtonElement = document.getElementById('fixedIqamaIshaButton');


	if(JS_DATA.ucJomoaFixedTime == "") jomoaFixedTimeButtonElement.innerHTML = "AUTO"; else jomoaFixedTimeButtonElement.innerHTML = JS_DATA.ucJomoaFixedTime;
		
	if(JS_DATA.ucFixedIqamaFAJR == "") fixedIqamaFajrButtonElement.innerHTML = "-- --"; else fixedIqamaFajrButtonElement.innerHTML = JS_DATA.ucFixedIqamaFAJR;
	if(JS_DATA.ucFixedIqamaDOHR == "") fixedIqamaDohrButtonElement.innerHTML = "-- --"; else fixedIqamaDohrButtonElement.innerHTML = JS_DATA.ucFixedIqamaDOHR;
	if(JS_DATA.ucFixedIqamaASSR == "") fixedIqamaAsrButtonElement.innerHTML = "-- --"; else fixedIqamaAsrButtonElement.innerHTML = JS_DATA.ucFixedIqamaASSR;
	if(JS_DATA.ucFixedIqamaISHA == "") fixedIqamaIshaButtonElement.innerHTML = "-- --"; else fixedIqamaIshaButtonElement.innerHTML = JS_DATA.ucFixedIqamaISHA;

}




function showInitialMessagesFunction()
{
if(isTestModeActive) return;
setTimeout(showMessagePopupFunction, 0, decodeHexStringFunction(encodedString2), 0.15);
}



function editJomoaFixedTimeFunction()
{
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_FixedTimeForJOMOA+JS_YOU_USE_24HOURS, JS_DATA.ucJomoaFixedTime, 'editJomoaFixedTimeFunction()', true);
	return;
	}
	
	if(inputDialogValue.length != 5) inputDialogValue = "AUTO";
	JS_DATA.ucJomoaFixedTime = inputDialogValue;
	jomoaFixedTime = JS_DATA.ucJomoaFixedTime;
	updateFixedTimesDisplayFunction();
	saveSettingsToStorageFunction();
	updateAthanIqamaDisplayFunction();
	calculateAndDisplayTimesFunction();
}




function editFixedIqamaFajrFunction()
{

	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_FixedTimeForIQAMAT+prayerNamesArray[0]+JS_YOU_USE_24HOURS, JS_DATA.ucFixedIqamaFAJR, 'editFixedIqamaFajrFunction()', true);
	return;
	}
			
	if(inputDialogValue.length != 5) inputDialogValue = "";
	JS_DATA.ucFixedIqamaFAJR = inputDialogValue;
	if(JS_DATA.ucFixedIqamaFAJR == "") JS_DATA.ucIqamaFAJR = 10;
	updateFixedTimesDisplayFunction();
	saveSettingsToStorageFunction();
	updateAthanIqamaDisplayFunction();
	calculateAndDisplayTimesFunction();
}




function editFixedIqamaDohrFunction()
{
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_FixedTimeForIQAMAT+prayerNamesArray[2]+JS_YOU_USE_24HOURS, JS_DATA.ucFixedIqamaDOHR, 'editFixedIqamaDohrFunction()', true);
	return;
	}
	
	if(inputDialogValue.length != 5) inputDialogValue = "";
	JS_DATA.ucFixedIqamaDOHR = inputDialogValue;
	if(JS_DATA.ucFixedIqamaDOHR == "") JS_DATA.ucIqamaDOHR = 10;
	updateFixedTimesDisplayFunction();
	saveSettingsToStorageFunction();
	updateAthanIqamaDisplayFunction();
	calculateAndDisplayTimesFunction();
}




function editFixedIqamaAsrFunction()
{
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_FixedTimeForIQAMAT+prayerNamesArray[3]+JS_YOU_USE_24HOURS, JS_DATA.ucFixedIqamaASSR, 'editFixedIqamaAsrFunction()', true);
	return;
	}
	
	if(inputDialogValue.length != 5) inputDialogValue = "";
	JS_DATA.ucFixedIqamaASSR = inputDialogValue;
	if(JS_DATA.ucFixedIqamaASSR == "") JS_DATA.ucIqamaASSR = 10;
	updateFixedTimesDisplayFunction();
	saveSettingsToStorageFunction();
	updateAthanIqamaDisplayFunction();
	calculateAndDisplayTimesFunction();
}




function editFixedIqamaIshaFunction()
{
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_FixedTimeForIQAMAT+prayerNamesArray[5]+JS_YOU_USE_24HOURS, JS_DATA.ucFixedIqamaISHA, 'editFixedIqamaIshaFunction()', true);
	return;
	}
	
	if(inputDialogValue.length != 5) inputDialogValue = "";
	JS_DATA.ucFixedIqamaISHA = inputDialogValue;
	if(JS_DATA.ucFixedIqamaISHA == "") JS_DATA.ucIqamaISHA = 10;
	updateFixedTimesDisplayFunction();
	saveSettingsToStorageFunction();
	updateAthanIqamaDisplayFunction();
	calculateAndDisplayTimesFunction();
}








function populateCountryListFunction()
{
let countryOptionsHtml = "";
let cityCodeParts = JS_DATA.ucNowCityCODE.split(".");

cityButtonElement.innerHTML = capitalizeFirstLetterFunction(cityCodeParts[1]);

	for(let i = 0; i < JS_WORLD_COUNTRIES.length; i++)
	{
		let countryDataParts = JS_WORLD_COUNTRIES[i].split("|");
		let countryCode = countryDataParts[0];
		let countryName = countryDataParts[1];
		countryOptionsHtml += "<button id='jCN_" +countryCode+ "' onclick=\"loadCountryCitiesFunction('" +countryCode+ "');\">"+countryName+"</button><br>";
		if(countryCode == cityCodeParts[0].toUpperCase()) countryButtonElement.innerHTML = countryName;
	}
countryListContainerElement.innerHTML = countryOptionsHtml;
}



function loadCountryCitiesFunction(selectedCountryCode)
{
hideElementByIdFunction("countryListModalId");
cityListContainerElement.innerHTML  = ""; 
cityListContainerElement.innerHTML += "<button id='NX_citi_00' onclick=\"selectCityFunction('00.000');\">00.000.PERSONAL</button><br>";

	const cacheBustingTimestamp = new Date().getTime();
	countryButtonElement.innerHTML	= document.getElementById('jCN_'+selectedCountryCode).innerHTML;
	cityButtonElement.innerHTML	= "&nbsp;";
	locationLoadingIndicator.innerHTML = 'LOADING ...';
	
		let countryDataScriptUrl = "data/" + selectedCountryCode + "/" + selectedCountryCode.toLowerCase() + ".js?ev=" + cacheBustingTimestamp;
		let countryScriptElement			= document.createElement("script");
		    countryScriptElement.type 		= "text/javascript";
			countryScriptElement.src 		= countryDataScriptUrl;
			countryScriptElement.onload	= function() { onCountryDataLoadSuccess(); };
			countryScriptElement.onerror	= function() { onCountryDataLoadError(); };
			document.body.appendChild(countryScriptElement);
		


}


function capitalizeFirstLetterFunction(cityNamePart) 
{
  return cityNamePart.charAt(0).toUpperCase() + cityNamePart.slice(1);
}



function onCountryDataLoadSuccess()
{


	let cityDataLowercase = '';
	for(let i = 0; i < JS_CITIES_DATA.length; i++) 
	{
		cityDataLowercase = JS_CITIES_DATA[i].toLowerCase();
		let countryDataParts = cityDataLowercase.split(".");
		let cityCode = countryDataParts[0] + "." + countryDataParts[1];
		let cityDisplayName = capitalizeFirstLetterFunction(countryDataParts[1]);
		if(countryDataParts[2] !== '---') cityDisplayName += '&nbsp;&nbsp;&nbsp;' + countryDataParts[2];
		cityListContainerElement.innerHTML += "<button id='NX_citi_" +countryDataParts[0]+ "' onclick=\"selectCityFunction('" +cityCode+ "');\">" +cityDisplayName+ "</button><br>";
		if(cityCode == JS_DATA.ucNowCityCODE) cityButtonElement.innerHTML = cityCode;
	}

onCountryDataLoadError();
}


function onCountryDataLoadError()
{
locationLoadingIndicator.innerHTML = '';
cityButtonElement.innerHTML = "- - - - - - -";
}



function openCityListFunction()
{
	if(cityListContainerElement.innerHTML == "") 
	{
	loadCountryCitiesFunction(JS_DATA.ucNowCityCODE.split(".")[0].toUpperCase()); 
	}
	else showElementFunction("cityListModalId");
}


function selectCityFunction(selectedCityCode) 
{

	JS_DATA.ucNowCityCODE = selectedCityCode;
	
	saveSettingsToStorageFunction();
	const timestampParam = new Date().getTime();
	let cityCodePartsArray = JS_DATA.ucNowCityCODE.split(".");
	cityButtonElement.innerHTML = capitalizeFirstLetterFunction(cityCodePartsArray[1]);
	
	let prayerTimesScriptUrl = 'data/' + cityCodePartsArray[0].toUpperCase() + '/wtimes-' + JS_DATA.ucNowCityCODE + '.js?e=' + timestampParam;
	let timesScriptElement = document.createElement("script");
	timesScriptElement.type = "text/javascript";
	timesScriptElement.src = prayerTimesScriptUrl;
	timesScriptElement.onload = function()
	{
		calculateAndDisplayTimesFunction();
	};
	document.body.appendChild(timesScriptElement);

hideElementByIdFunction("cityListModalId");
}




function changeAyatsLanguageFunction(selectedLanguageCode)
{
	JS_DATA.ucAyatsLANG = selectedLanguageCode;
	saveSettingsToStorageFunction();
	setTimeout('restartApplicationReload()', 300);
}



function changeLanguageFunction(languageSpanElement)
{
	JS_DATA.ucLangNOW = languageSpanElement.id;
	saveSettingsToStorageFunction();
	setTimeout('restartApplicationReload()', 300);
}


function restartApplicationFunction() 
{
setTimeout('restartApplicationReload()', 100);
}



function forcedVerticalModeParam(forcedVerticalModeParam)
{
	JS_DATA.ucForcedVertical = forcedVerticalModeParam;
	saveSettingsToStorageFunction();
	setTimeout('restartApplicationReload()', 300);
}




let blackScreenCountdown = 15;
function animateBlackScreenCountdown()
{
	blackScreenCountdown--;

	if(blackScreenCountdown < 1)
	{
		blackScreenCountdown = 15;
		setTimeout(hideElementByIdFunction, 5000, 'noMobileImageVertical');
		setTimeout(hideElementByIdFunction, 5000, 'noMobileImageHorizontal');
		return;
	}

	if(blackScreenCountdown % 2 == 0)
	{
		noMobileImageVerticalElement.style.visibility = 'hidden';
		noMobileImageHorizontalElement.style.visibility = 'hidden';
	}
	else
	{
		noMobileImageVerticalElement.style.visibility = 'visible';
		noMobileImageHorizontalElement.style.visibility = 'visible';
	}

	setTimeout('animateBlackScreenCountdown()', 700);
}




function startIqamaSequenceFunction()
{

	if(prayerDurationMinutes == 0)
	{
		showElementWithTransitionFunction('dohaPopupVertical');
		showElementWithTransitionFunction('dohaPopupHorizontal');
		setTimeout(hideElementFunction, 20000, 'dohaPopupVertical');
		setTimeout(hideElementFunction, 20000, 'dohaPopupHorizontal');

		if(JS_DATA.ucSlidesActive == 1) setTimeout("startSlidesSequenceFunction()", 30000);
		return;
	}

	setTimeout('onIqamaEndFunction()', (prayerDurationMinutes * 60000));

	let delayBeforeBlackScreen = 12000; 

	if(JS_DATA.ucBlackScreenInPraying == 1)
	{
		if(JS_DATA.ucShowIqamaScreen == 0) delayBeforeBlackScreen = 0;
		setTimeout('activateBlackScreenIfEnabled()', delayBeforeBlackScreen);
	}

	if(JS_DATA.ucShowIqamaScreen == 1)
	{
		showElementWithTransitionFunction('iqamaPopupVertical');
		showElementWithTransitionFunction('iqamaPopupHorizontal');
		setTimeout(hideElementFunction, (delayBeforeBlackScreen - 2000), 'iqamaPopupVertical');
		setTimeout(hideElementFunction, (delayBeforeBlackScreen - 2000), 'iqamaPopupHorizontal');
	}

	if(currentPrayerIndex == 1) return; 
	if(JS_DATA.ucBlackScreenInPraying == 1) setTimeout('animateBlackScreenCountdown()', delayBeforeBlackScreen + 3000); 
}




function onIqamaEndFunction()
{
	deactivateBlackScreen();

	if(JS_DATA.ucAzkarActive == 1)
	{
		setTimeout('startAzkarMainSequenceFunction()', 7000);
	}
	else
	{
		isAzkarMain = true;
		decideNextAzkarOrSlideFunction();
	}
}






let stopAzkarFlag = false;
let stopSlidesFlag = false;

let  decodedStringDisplayElement = document.createElement(divTagName); 
 decodedStringDisplayElement.id = "decodedStringDisplayId";
menuContentElement.appendChild( decodedStringDisplayElement);



function showAzkarDisplayFunction()
{

prepareAzkarDisplayFunction();
	
	showFullScreenElementFunction('azkarContainerVertical');
	showFullScreenElementFunction('azkarContainerHorizontal');

	showElementFunction('azkarTextDisplayHorizontal');
	showElementFunction('azkarTextDisplayVertical');
}




function hideAzkarDisplayFunction()
{
	hideFullScreenElementFunction('azkarContainerVertical');
	hideFullScreenElementFunction('azkarContainerHorizontal');

cleanupAzkarDisplayFunction();	
}


function prepareAzkarDisplayFunction()
{
isBigCounterActive = true;


showPrayerTimesOverlayFunction(false);
}


function cleanupAzkarDisplayFunction()
{
isBigCounterActive = false;


hidePrayerTimesOverlayFunction();
}


function showPrayerTimesOverlayFunction(isFullScreenCounterActive)
{

	if(isHorizontalOrientation)
	{
	hadithDisplayHorizontalElement.style.zIndex = 1000;
	marqueeContainerHorizontalElement.style.zIndex = 1000;
	setTimeout(showElementWithTransitionFunction, 1000, 'miniPrayerTimesHorizontal');
	}
else
	{
	hadithDisplayVerticalElement.style.zIndex = 1000;
	marqueeContainerVerticalElement.style.zIndex = 1000;
		if(isFullScreenCounterActive)
		{
		     if(verticalMainContainer.className == '')		rootElement.style.setProperty('--eBtmMiniTIMES', '10.5%');
		else if(verticalMainContainer.className == 'specialVrClass')	rootElement.style.setProperty('--eBtmMiniTIMES', '15%');
		}
	setTimeout(showElementWithTransitionFunction, 1000, 'miniPrayerTimesVertical');
	}

}



function hidePrayerTimesOverlayFunction()
{

	if(isHorizontalOrientation)
	{
	hideElementFunction('miniPrayerTimesHorizontal');
	hadithDisplayHorizontalElement.style.zIndex = 300;
	marqueeContainerHorizontalElement.style.zIndex = 300;	
	}
else
	{
	hideElementFunction('miniPrayerTimesVertical');
	hadithDisplayVerticalElement.style.zIndex = 300;
	marqueeContainerVerticalElement.style.zIndex = 300;	
	rootElement.style.setProperty('--eBtmMiniTIMES', '8.8%');
	}	
}
			


function showSlidesDisplayFunction()
{
	if(isHorizontalOrientation)
	{
	showFullScreenElementFunction('slidesContainerHorizontal');
	}
else
	{
	showFullScreenElementFunction('slidesContainerVertical');
	}
}

function hideSlidesDisplayFunction()
{
	if(isHorizontalOrientation)
	{
	hideFullScreenElementFunction('slidesContainerHorizontal');
	}
else
	{
	hideFullScreenElementFunction('slidesContainerVertical');
	}
}





function buildAzkarArraysFunction()
{

	let azkarBlockString = "";
	let sourceAzkarArray = [];
	if(isHorizontalOrientation) sourceAzkarArray = JS_GLOBAL_AZKAR_HR; else sourceAzkarArray = JS_GLOBAL_AZKAR_VR;
	

	for(let i = 0; i < sourceAzkarArray.length; i++)
	{
		if(sourceAzkarArray[i] !== "")
		{
		let azkarLine = sourceAzkarArray[i];
			if(azkarLine.indexOf('-----') > -1)
			{
			if(azkarBlockString !== "") mainAzkarArray.push(azkarBlockString);
			azkarBlockString = "";
			}
			else {azkarBlockString += azkarLine + "<br>";}
		}
	}


	sourceAzkarArray = JS_GLOBAL_AZKAR_SABAH;

	for(let i = 0; i < sourceAzkarArray.length; i++)
	{
		if(sourceAzkarArray[i] !== "")
		{
			let azkarLine = sourceAzkarArray[i];
			if(azkarLine.indexOf('-----') > -1)
			{
				if(azkarBlockString !== "") sabahAzkarArray.push(azkarBlockString);
				azkarBlockString = "";
			}
			else
			{
				azkarBlockString += azkarLine + "<br>";
			}
		}
	}

	sourceAzkarArray = JS_GLOBAL_AZKAR_MASAA;

	for(let i = 0; i < sourceAzkarArray.length; i++)
	{
		if(sourceAzkarArray[i] !== "")
		{
			let azkarLine = sourceAzkarArray[i];
			if(azkarLine.indexOf('-----') > -1)
			{
				if(azkarBlockString !== "") masaaAzkarArray.push(azkarBlockString);
				azkarBlockString = "";
			}
			else
			{
				azkarBlockString += azkarLine + "<br>";
			}
		}
	}


mobileAlertButtonElement.innerHTML = JS_DATA.ucCloseMobilePlease;
displayRandomHadithFunction();

document.getElementById('eidFitrTimeButton').innerHTML = JS_DATA.ucTimeOfEidFITR;
document.getElementById('eidAdhaTimeButton').innerHTML = JS_DATA.ucTimeOfEidADHA;


}





function startAzkarSequenceFunction(azkarArrayToDisplay, isMainAzkar, isSabahAzkar)
{

	currentAzkarIndex = 0; 
	azkarDisplayArray = [];

	for(let i = 0; i < azkarArrayToDisplay.length; i++)
	{
	azkarDisplayArray.push(azkarArrayToDisplay[i]);
	}

	isAzkarMain = isMainAzkar;
	
	azkarDisplayInterval = (JS_SabahMasaaViewTime * 1000);
	if(isMainAzkar) 
	azkarDisplayInterval = (JS_MainAzkarViewTime * 1000);
	
	isSabahFlag = isSabahAzkar;
	showAzkarDisplayFunction();
	displayNextAzkarFunction();
}




function stopAzkarDisplayFunction()
{

	hideElementByIdFunction('azkarTextDisplayHorizontal');
	hideElementByIdFunction('azkarTextDisplayVertical');

	hideAzkarDisplayFunction();
	stopAzkarFlag = true;
	currentAzkarIndex = 0;
}




function stopSlidesDisplayFunction()
{
	slidesDisplayActive = false;
	stopSlidesFlag = true;
	currentSlideIndex = 0;
	hideSlidesDisplayFunction();
}





const azkarTextDisplayVerticalElement = document.getElementById('azkarTextDisplayVertical');
const azkarTextDisplayHorizontalElement = document.getElementById('azkarTextDisplayHorizontal');
let isAzkarMain = false;
let isSabahFlag = false;

function decideNextAzkarOrSlideFunction()
{

	
	if((currentPrayerIndex == 0) && (JS_DATA.ucAzkarSabahOn == 1) && (isAzkarMain))
	{
		startAzkarSequenceFunction(sabahAzkarArray, false, true);
	}
	else
	if((currentPrayerIndex == 3) && (JS_DATA.ucAzkarAsrOn == 1) && (isAzkarMain))
	{
		startAzkarSequenceFunction(masaaAzkarArray, false, false);
	}
	else
	if((currentPrayerIndex == 4) && (JS_DATA.ucAzkarMaghribOn == 1) && (isAzkarMain))
	{
		startAzkarSequenceFunction(masaaAzkarArray, false, false);
	}
	else
	if((currentPrayerIndex == 5) && (JS_DATA.ucAzkarIshaOn == 1) && (isAzkarMain))
	{
		startAzkarSequenceFunction(masaaAzkarArray, false, false);
	}
	else
	if(JS_DATA.ucSlidesActive == 1) setTimeout("startSlidesSequenceFunction()", 7000);

}



let azkarDisplayInterval = 20000;

function displayNextAzkarFunction()
{


let skipEmptyAzkar = false;

	if(stopAzkarFlag)
	{
		stopAzkarFlag = false;
		return;
	}

	if(currentAzkarIndex >= azkarDisplayArray.length)
	{
		currentAzkarIndex = 0;
		hideAzkarDisplayFunction();
		setTimeout("decideNextAzkarOrSlideFunction()", 7000);
		return;
	}

	
	if(isAzkarMain)
	{
		if(   (currentPrayerIndex !== 0) && (currentAzkarIndex== 7)   ) skipEmptyAzkar = true;
		if( ( (currentPrayerIndex !== 0) && (currentPrayerIndex !== 4) ) && (currentAzkarIndex==8)   ) skipEmptyAzkar = true;
	}


				if(skipEmptyAzkar)
				{
				currentAzkarIndex++;
				displayNextAzkarFunction();
				return;
				}

	
	
	let currentAzkarText = azkarDisplayArray[currentAzkarIndex];
	let azkarTextLength   = countArabicCharactersFunction(currentAzkarText);
	
	



 let azkarFontSize = 5;


	
	if(!isHorizontalOrientation)
	{
	azkarFontSize = calculateFontSizeFunction(azkarTextLength, 500, 50, 10.4, 6.1);
	}
else
	{
	azkarFontSize = calculateFontSizeFunction(azkarTextLength, 500, 50, 6.7, 2.8);
	}
	
	
	azkarFontSizeVr = azkarFontSize;
	azkarFontSizeHr = azkarFontSize;
	azkarTextDisplayHorizontalElement.style.lineHeight = '1.70em';
	
	azkarTextDisplayHorizontalElement.style.fontSize = 'calc('+azkarFontSizeVr+fontSizeUnit+' - var(--NowSULTAN) ) ';
	azkarTextDisplayVerticalElement.style.fontSize = 'calc('+azkarFontSizeHr+fontSizeUnit+' - var(--NowSULTAN) ) ';

	
	currentAzkarText = currentAzkarText.replace(/33/g, "<span>33</span>");
	currentAzkarText = currentAzkarText.replace(/\|/g, "<br>");
	azkarTextDisplayVerticalElement.innerHTML = currentAzkarText;
	azkarTextDisplayHorizontalElement.innerHTML = currentAzkarText;
	azkarTextDisplayVerticalElement.style.lineHeight = azkarTextDisplayHorizontalElement.style.lineHeight;
	currentAzkarIndex++;
	fadeInAzkarTextFunction();
	setTimeout("displayNextAzkarFunction()", azkarDisplayInterval); 
	setTimeout("fadeOutAzkarTextFunction()",      azkarDisplayInterval - 6000);
}



function calculateFontSizeFunction(textLength, maxTextLength, minTextLength, maxFontSize, minFontSize)
{

let minTextLengthParam = minTextLength;
let textLengthRange = (maxTextLength - minTextLengthParam); 
let fontSizeRange = (maxFontSize - minFontSize); 


let textLengthDelta = (textLength - minTextLengthParam); 
let textLengthPercent = ((textLengthDelta * 100)/textLengthRange); 
let fontSizeDelta = ((textLengthPercent * fontSizeRange)/100); 
let calculatedFontSize = (maxFontSize - fontSizeDelta).toFixed(2);
return calculatedFontSize;
}



function fadeInAzkarTextFunction()
{
	fadeInElementFunction('azkarTextDisplayHorizontal');
	fadeInElementFunction('azkarTextDisplayVertical');
}


function fadeOutAzkarTextFunction()
{
	fadeOutElementFunction('azkarTextDisplayHorizontal');
	fadeOutElementFunction('azkarTextDisplayVertical');
}


function setElementAttributeFunction()
{
prayerTimesContainerVerticalElement.setAttribute('i'+attributeName,attributeValue);
prayerTimesContainerHorizontalElement.setAttribute('i'+attributeName,attributeValue);
prayerTimesTableHorizontalElement.setAttribute('i'+attributeName,attributeValue);
}


function clampSlidesViewTimeFunction()
{
if(JS_DATA.ucSlidesViewTime < 10) JS_DATA.ucSlidesViewTime = 10;
if(JS_DATA.ucTawkitViewTime < 10) JS_DATA.ucTawkitViewTime = 10;

if(JS_DATA.ucSlidesViewTime > 60) JS_DATA.ucSlidesViewTime = 60;
if(JS_DATA.ucTawkitViewTime > 60) JS_DATA.ucTawkitViewTime = 60;
}


const slidesTextVerticalElement = document.getElementById('slidesTextDisplayVertical');
const slidesTextHorizontalElement = document.getElementById('slidesTextDisplayHorizontal');
let slidesDisplayActive = false;


function displayNextSlideFunction()
{

	let currentSlideText = '';
	
	if(JS_DATA.ucSlidesRandom == 1)
	currentSlideText = JS_SLIDES_DATA[Math.floor((Math.random() * JS_SLIDES_DATA.length))];
	else
	currentSlideText = JS_SLIDES_DATA[currentSlideIndex];
	
	clampSlidesViewTimeFunction();
		

	let slideViewTimeMs = (JS_DATA.ucSlidesViewTime * 1000);
	let tawkitViewTimeMs = (JS_DATA.ucTawkitViewTime * 1000);


	if(currentSlideText == "")
	{
		currentSlideIndex++;
		displayNextSlideFunction();
		return;
	}

	if(stopSlidesFlag)
	{
		stopSlidesFlag = false; 
		return;
	}

	
	
	if(currentSlideIndex >= JS_SLIDES_DATA.length)
	{
		currentSlideIndex = 0;
		displayNextSlideFunction();
		return;
	}


let isImageSlide = ( (currentSlideText.indexOf('.jpg') > -1) || (currentSlideText.indexOf('.png') > -1) );
let isUrlSlide =   (currentSlideText.indexOf('http') == 0);

	if( (isImageSlide) || (isUrlSlide) )
	{
		currentSlideIndex++;
		slidesTextVerticalElement.innerHTML = '';
		slidesTextHorizontalElement.innerHTML = '';
		
		slidesContainerVerticalElement.style.backgroundColor = 'transparent';
		slidesContainerHorizontalElement.style.backgroundColor = 'transparent';	
		
		let slideBackgroundCss = '';
		if(isUrlSlide) slideBackgroundCss = 'url(' + currentSlideText + ')';
					else slideBackgroundCss = 'url(./slides/' + currentSlideText + ')';
		
		slidesContainerVerticalElement.style.backgroundImage = slideBackgroundCss;
		slidesContainerVerticalElement.style.backgroundRepeat = 'no-repeat';
		slidesContainerVerticalElement.style.backgroundSize = '100% 100%';
		slidesContainerHorizontalElement.style.backgroundImage = slideBackgroundCss;
		slidesContainerHorizontalElement.style.backgroundRepeat = 'no-repeat';
		slidesContainerHorizontalElement.style.backgroundSize = '100% 100%';
		showSlidesDisplayFunction();
		
		setTimeout("displayNextSlideFunction()", tawkitViewTimeMs + 5000 + slideViewTimeMs); 
		setTimeout("hideSlidesDisplayFunction()", slideViewTimeMs);
		return;
	}
	else
	{
		
		slidesContainerVerticalElement.style.backgroundColor = '#121212';
		slidesContainerVerticalElement.style.background = 'url(' + slidesFolderPath + '.jpg)';
		slidesContainerVerticalElement.style.backgroundRepeat = 'no-repeat';
		slidesContainerVerticalElement.style.backgroundSize = '100% 100%';
		
		slidesContainerHorizontalElement.style.backgroundColor = '#121212';
		slidesContainerHorizontalElement.style.background = 'url(' + slidesFolderPath + '.jpg)';
		slidesContainerHorizontalElement.style.backgroundRepeat = 'no-repeat';
		slidesContainerHorizontalElement.style.backgroundSize = '100% 100%';

	}


	
	let azkarTextLength = countArabicCharactersFunction(currentSlideText);
	currentSlideText = currentSlideText.replace(/\|/g, "<br>");


let slidesFontSize = 5;


	if(!isHorizontalOrientation)
	{
	slidesFontSize = calculateFontSizeFunction(azkarTextLength, 500, 50, 9.5, 4.5);
	
	}
else
	{
	slidesFontSize = calculateFontSizeFunction(azkarTextLength, 500, 50, 6.7, 2.8);
	
	}
	
	
	
	azkarFontSizeVr = slidesFontSize;
	azkarFontSizeHr = slidesFontSize;
	slidesTextHorizontalElement.style.lineHeight = '1.70em';







	slidesTextHorizontalElement.style.fontSize = 'calc('+azkarFontSizeVr+fontSizeUnit+' - var(--NowSULTAN) ) ';
	slidesTextVerticalElement.style.fontSize = 'calc('+azkarFontSizeHr+fontSizeUnit+' - var(--NowSULTAN) ) ';
	
	slidesTextVerticalElement.innerHTML = currentSlideText;
	slidesTextHorizontalElement.innerHTML = currentSlideText;
	
	slidesTextVerticalElement.style.lineHeight = slidesTextHorizontalElement.style.lineHeight;


	currentSlideIndex++;
	showSlidesDisplayFunction();
	setTimeout("displayNextSlideFunction()", tawkitViewTimeMs + 5000 + slideViewTimeMs); 
	setTimeout("hideSlidesDisplayFunction()", slideViewTimeMs);

}




function startAzkarMainSequenceFunction()
{
	stopAzkarFlag = false;

	if(JS_DATA.ucAzkar5minPicture == 1)
	{
		showAzkar5minPictureFunction();
		return;
	}

	startAzkarSequenceFunction(mainAzkarArray, true, false);
}




function showAzkar5minPictureFunction()
{

let nbrBG = 0; 
if(JS_DATA.ucAzkarBGwhite) nbrBG = 1; 

		if( (JS_DATA.ucUserFilesACTIVE == 1) && (userUploadedImages[1]) )
		{
		document.getElementById('azkar5minPictureContainerHorizontal').style.backgroundImage = "url("+userUploadedImages[1]+")";
		document.getElementById('azkar5minPictureContainerVertical').style.backgroundImage = "url("+userUploadedImages[1]+")";
		}
		else
		{
		document.getElementById('azkar5minPictureContainerHorizontal').style.backgroundImage = "url(./azkar/azkar5min-hr-"+nbrBG+".jpg)";
		document.getElementById('azkar5minPictureContainerVertical').style.backgroundImage = "url(./azkar/azkar5min-vr-"+nbrBG+".jpg)";
		}



   
	if(JS_5minAzkarSemiTransparent == 1)
	{
	showSemiTransparentElementFunction('azkar5minPictureContainerVertical');
	showSemiTransparentElementFunction('azkar5minPictureContainerHorizontal');
	}
	else
	{
	showFullScreenElementFunction('azkar5minPictureContainerVertical');
	showFullScreenElementFunction('azkar5minPictureContainerHorizontal');
	}

	setTimeout("hideAzkar5minPictureFunction()", (60000 * 5)); 
}




function hideAzkar5minPictureFunction()
{
	hideElementFunction('azkar5minPictureContainerHorizontal');
	hideElementFunction('azkar5minPictureContainerVertical');
	isAzkarMain = true;
	setTimeout("decideNextAzkarOrSlideFunction()", 7000); 
}




function startSlidesSequenceFunction()
{
	if(slidesDisplayActive) return; 
	slidesDisplayActive = true;
	stopSlidesFlag = false;
	displayNextSlideFunction();
}




function deactivateBlackScreen()
{
	blackScreenVerticalElement.style.transform = 'scaleX(0)';
	blackScreenHorizontalElement.style.transform = 'scaleX(0)';
}




function closeFullScreenCounterFunction()
{
	isFullScreenCounterMode = false;
	rootElement.style.setProperty('--izBigCNTR', 1);
	hideElementFunction('fullScreenCounterContainerVertical');
	hideElementFunction('fullScreenCounterContainerHorizontal');
	hideElementByIdFunction('noMobileImageContainerHorizontal');
	ayaContainerVertical.style.top = '40.5%';
	ayaContainerVertical.style.zIndex = 300;
	messageContainerVertical.style.top = '40.5%';
	messageContainerVertical.style.zIndex = 300;
	hadithDisplayHorizontalElement.style.zIndex = 300;
	marqueeContainerHorizontalElement.style.zIndex = 300;
	staticMessageHorizontalElement.style.zIndex = 299;
	hidePrayerTimesOverlayFunction();
}




function hideIqamaCounterFunction()
{
	hideElementFunction('iqamaCounterContainerHorizontal');
	hideElementFunction('iqamaCounterContainerVertical');
}




function activateBlackScreenIfEnabled()
{
	if(JS_DATA.ucBlackScreenInPraying == 1) activateBlackScreenFunction();
}



function activateBlackScreenFunction()
{
	stopAzkarDisplayFunction();
	stopSlidesDisplayFunction();
	nightPrayersClickCounter = 0;
	
blackScreenVerticalElement.style.transform = 'scaleX(1)';
blackScreenHorizontalElement.style.transform = 'scaleX(1)';
}


function setElementAttribute2Function()
{
prayerTimesContainerVerticalElement.setAttribute('i'+attributeName,attributeValue);
prayerTimesContainerHorizontalElement.setAttribute('i'+attributeName,attributeValue);
prayerTimesTableHorizontalElement.setAttribute('i'+attributeName,attributeValue);
}


function showElementFunction(elementIdParam)
{
	document.getElementById(elementIdParam).style.visibility = 'visible';
}

function hideElementByIdFunction(elementIdParam)
{
	document.getElementById(elementIdParam).style.visibility = 'hidden';
}



function showElementBlockFunction(elementIdParam)
{
	document.getElementById(elementIdParam).style.display = 'block';
}

function hideElementDisplayFunction(elementIdParam)
{
	document.getElementById(elementIdParam).style.display = 'none';
}



let circleProgressPercent = 0;
let isLastMinuteCounter = false;

const fullClockSecondsVertical = document.getElementById('fullClockSecondsDisplayVertical');
const fullClockSecondsHorizontal = document.getElementById('fullClockSecondsDisplayHorizontal');
const miniClockSecondsVertical = document.getElementById('miniClockSecondsDisplayHorizontal');
const miniClockSecondsHorizontal = document.getElementById('miniClockSecondsDisplayVertical');

const fullClockAmPmVertical = document.getElementById('fullClockAmPmDisplayVertical');
const fullClockAmPmHorizontal = document.getElementById('fullClockAmPmDisplayHorizontal');


const attributeName = 'd';
const attributeValue = mainMenuElement.id;


let currentDateString = "";

const specificSecondsTrigger = numbersAsStringsArray[0]+numbersAsStringsArray[4];

function clockTickFunction()
{

	let currentDateObject = new Date();
	
	if( (JS_DATA.ucLocalHoursAdjustment !== 0) && (allowedHourAdjustments.indexOf(JS_DATA.ucLocalHoursAdjustment) !== -1) )
	{currentDateObject.setTime(currentDateObject.getTime() + (JS_DATA.ucLocalHoursAdjustment*60*60*1000));}
	
	
	let currentSeconds = currentDateObject.getSeconds();
	currentSeconds = ('0' + currentSeconds).slice(-2);
	
	if((currentSeconds==specificSecondsTrigger)&&(isSpecialTriggerActive)) setElementAttributeFunction();

	let formattedSeconds = convertToArabicDigitsFunction(currentSeconds);

	fullClockSecondsVertical.innerHTML = formattedSeconds;
	fullClockSecondsHorizontal.innerHTML = formattedSeconds;

	miniClockSecondsVertical.innerHTML = ':' + formattedSeconds;
	miniClockSecondsHorizontal.innerHTML = ':' + formattedSeconds;

	if(isAzanPopupVisible)
	{
	azanPopupTimeVerticalElement.innerHTML = currentTimeFormatted;
	azanPopupTimeHorizontalElement.innerHTML = currentTimeFormatted;
	}
	
	
	
	
	
	
	
						if(isIqamaCounterActive)
						{
					
									remainingSeconds--;
							
									circleProgressPercent = totalCounterSeconds - remainingSeconds;
									circleProgressPercent = Math.floor((circleProgressPercent*100/totalCounterSeconds));
									
									iqamaCounterCircleVerticalElement.style.strokeDasharray = [(100 - circleProgressPercent),100];
									iqamaCounterCircleHorizontalElement.style.strokeDasharray = iqamaCounterCircleVerticalElement.style.strokeDasharray;

									
									let remainingMinutes = Math.floor(remainingSeconds / 60);
									let remainingSecondsPart = Math.floor(remainingSeconds % 60);
									remainingMinutes = ('0' + remainingMinutes).slice(-2);
									remainingSecondsPart = ('0' + remainingSecondsPart).slice(-2);
							
									if(isFullScreenCounterMode)
									{
										fullScreenTimeHorizontal.innerHTML = convertToArabicDigitsFunction(remainingMinutes + ':' + remainingSecondsPart);
										fullScreenTimeVertical.innerHTML = fullScreenTimeHorizontal.innerHTML;
									}
									else
									{
										iqamaCounterTimeHorizontalElement.innerHTML = convertToArabicDigitsFunction(remainingMinutes + ':' + remainingSecondsPart);
										iqamaCounterTimeVerticalElement.innerHTML = iqamaCounterTimeHorizontalElement.innerHTML;
									}
									
									if( ((remainingSeconds == 90)&&(!isDohaCounter)&&(JS_DATA.ucCounterColorAlert == 1)) || (isCounterColorAlert) )
									{	
										iqamaCounterCircleVerticalElement.style.stroke = '#BA001F';
										iqamaCounterCircleHorizontalElement.style.stroke = '#BA001F';
										fullScreenTimeVertical.style.color = '#BA001F';
										fullScreenTimeHorizontal.style.color = '#BA001F';
										isCounterColorAlert = false;
									}							
									
									if(isLastMinuteCounter)
									{
										secondCounterVerticalElement.innerHTML = convertToArabicDigitsFunction(remainingSecondsPart);
										secondCounterHorizontalElement.innerHTML = secondCounterVerticalElement.innerHTML;
									}
							
							
									if((remainingSeconds == 60)&&(!isDohaCounter))
									{
									
									showElementFunction('noMobileImageContainerHorizontal');
									

										if(JS_DATA.ucAlertLastMinute == 1)
										{
										setTimeout(showMessagePopupFunction, 1000, JS_DATA.ucCloseMobilePlease, 1);
										if(isTawkitApp) redirectToAudioHandlerFunction(audioAlert1ElementVar.dataset.xfile); else audioAlert1ElementVar.play();
										}											
									
										if(JS_DATA.ucCounterLastMinute == 1)
										{
										isLastMinuteCounter = true;
										showElementWithTransitionFunction('secondCounterContainerVertical');
										showElementWithTransitionFunction('secondCounterContainerHorizontal');
										}		
									}
							
									
									if((JS_DATA.ucIqamaHadith == 1)&&(!isDohaCounter)&&(remainingSeconds == 33))
									{
									showStaticMessageFunction(JS_IqamaRULE, 0.5);
									}
							
							
									if(remainingSeconds == 0)
									{
										isIqamaCounterActive = false; 
										isLastMinuteCounter = false;
										resetPrayerRowsStyleFunction();
										hideElementFunction('secondCounterContainerVertical');
										hideElementFunction('secondCounterContainerHorizontal');
										iqamaCounterCircleVerticalElement.style.stroke = 'var(--esLOWER)';
										iqamaCounterCircleHorizontalElement.style.stroke = 'var(--esLOWER)';
										fullScreenTimeVertical.style.color = 'var(--esSALATS)';
										fullScreenTimeHorizontal.style.color = 'var(--esSALATS)';
										setTimeout('startIqamaSequenceFunction()', 1000);
										if(isFullScreenCounterMode) setTimeout('closeFullScreenCounterFunction()',   1000);
															 else setTimeout('hideIqamaCounterFunction()', 1000);
									}
						}

	if(currentSeconds=='08') displayRandomAyaFunction();
	if(currentSeconds=='33') displayRandomHadithFunction();
	if(currentSeconds=='00') updateTimeAndPrayersFunction(false);
	
	
}






let amSymbolHtml = "<span class='amSymbolHtml'>AM</span>";
let pmSymbolHtml = "<span class='amSymbolHtml'>PM</span>";

let dohaMinutesAfterShrq = 30;
let ishaScreenSaverOffset = 60;

let blackScreenOffsetBeforeIsha = 60;
let blackScreenOffsetBeforeDoha = 60;
let midnightCounter = 0;


let isFriday = false;
let currentTimeFormatted = "00:00";



function updateTimeAndPrayersFunction(skipPrayerTimeChecks)
{

	let nowDateObject = new Date();

	if( (JS_DATA.ucLocalHoursAdjustment !== 0) && (allowedHourAdjustments.indexOf(JS_DATA.ucLocalHoursAdjustment) !== -1) )
	{nowDateObject.setTime(nowDateObject.getTime() + (JS_DATA.ucLocalHoursAdjustment*60*60*1000));}


	let currentHours = nowDateObject.getHours();
	let currentMinutes = nowDateObject.getMinutes();


	let hoursForAmPm = currentHours;
	let minutesFormatted = currentMinutes;
	minutesFormatted = ('0' + minutesFormatted).slice(-2);
	

	currentHours = ('0' + currentHours).slice(-2);
	currentMinutes = ('0' + currentMinutes).slice(-2);


	let timeString24 = currentHours + ':' + currentMinutes;

	let timeStringAmPm = timeString24;
	let amPmSymbol = '&nbsp;';




	if(!JS_DATA.ucActivate24Hours == 1)
	{
		if(hoursForAmPm >= 12)
		{
			amPmSymbol = pmSymbolHtml;
			if(hoursForAmPm > 12) hoursForAmPm = hoursForAmPm - 12;
			timeStringAmPm = hoursForAmPm + ':' + minutesFormatted;
		}
		else
		{
			if(hoursForAmPm == 0) hoursForAmPm = 12;
			amPmSymbol = amSymbolHtml;
			timeStringAmPm = hoursForAmPm + ':' + minutesFormatted;
		}
	}

	currentTimeFormatted = convertToArabicDigitsFunction(timeStringAmPm);
	
	fullClockTimeVerticalElement.innerHTML = currentTimeFormatted;
	fullClockTimeHorizontalElement.innerHTML = currentTimeFormatted;
	
	fullClockAmPmVertical.innerHTML = amPmSymbol;
	fullClockAmPmHorizontal.innerHTML = amPmSymbol;

	miniClockTimeHorizontalElement.innerHTML = currentTimeFormatted;
	miniClockTimeVerticalElement.innerHTML = currentTimeFormatted;


	azkarClockVerticalElement.innerHTML = currentTimeFormatted;
	azkarClockHorizontalElement.innerHTML = currentTimeFormatted;
	slidesClockVerticalElement.innerHTML = currentTimeFormatted;
	slidesClockHorizontalElement.innerHTML = currentTimeFormatted;
	picture5minClockVerticalElement.innerHTML = currentTimeFormatted;
	picture5minClockHorizontalElement.innerHTML = currentTimeFormatted;



	let todayDateString = nowDateObject.getDate()+'-'+(nowDateObject.getMonth()+1)+'-'+nowDateObject.getFullYear();
	if(todayDateString !== currentDateString) calculateAndDisplayTimesFunction();




	if(isFullScreenCounterMode)
	{
		fullScreenMiniClockHorizontal.innerHTML = currentTimeFormatted;
	}
	
	currentTimeInMinutes = ((parseInt(currentHours) * 60) + parseInt(currentMinutes));


	if(counterForDohaTimeDisplay > 3) checkForTawkitDisplayUpdate();


	let currentDayIndex = nowDateObject.getDay();
	isFriday = (currentDayIndex == 5);
	
	if(skipPrayerTimeChecks) return;
	




	if(currentTimeInMinutes == (shuruqTimeInMinutes + dohaMinutesAfterShrq))
	{
		if(JS_DATA.ucDohaScreenSaver == 1) activateBlackScreenFunction();
	}
	if(currentTimeInMinutes == (dohrTimeInMinutes - blackScreenOffsetBeforeDoha)) deactivateBlackScreen();

	if(currentTimeInMinutes == (ishaTimeInMinutes + ishaScreenSaverOffset))
	{
		if(JS_DATA.ucIshaScreenSaver == 1) activateBlackScreenFunction();
	}
	if(currentTimeInMinutes == (fajrTimeInMinutes - blackScreenOffsetBeforeIsha)) deactivateBlackScreen();


	 if((currentTimeInMinutes == (fajrTimeInMinutes - 3)) ||
		(currentTimeInMinutes == (shuruqTimeInMinutes - 3)) ||
		(currentTimeInMinutes == (dohrTimeInMinutes - 3)) ||
		(currentTimeInMinutes == (asrTimeInMinutes - 3)) ||
		(currentTimeInMinutes == (maghribTimeInMinutes - 3)) ||
		(currentTimeInMinutes == (ishaTimeInMinutes - 3))
	)
	{
		stopSlidesDisplayFunction();
		stopAzkarDisplayFunction();
	}

	
	if(currentTimeInMinutes == fajrTimeInMinutes) 	startIqamaCounterFunction(JS_DATA.ucIqamaFAJR, JS_DATA.ucPrayDurationFAJR, 0);
	if(currentTimeInMinutes == shuruqTimeInMinutes) 	startIqamaCounterFunction(JS_DATA.ucIqamaSHRQ, 0, 0);
	if(currentTimeInMinutes == asrTimeInMinutes) 	startIqamaCounterFunction(JS_DATA.ucIqamaASSR, JS_DATA.ucPrayDurationASSR, 0);
	if(currentTimeInMinutes == maghribTimeInMinutes) 	startIqamaCounterFunction(JS_DATA.ucIqamaMGRB, JS_DATA.ucPrayDurationMGRB, 0);
	if(currentTimeInMinutes == ishaTimeInMinutes) 	startIqamaCounterFunction(JS_DATA.ucIqamaISHA, JS_DATA.ucPrayDurationISHA, 0);


	if(isFriday)
	{
		
		if(currentTimeInMinutes == (dohrTimeInMinutes + JS_DATA.ucJomoaDimmBefore))
		{
		activateBlackScreenIfEnabled();
		}
		
		if(currentTimeInMinutes == (dohrTimeInMinutes + JS_DATA.ucJomoaDimmAfter))
		{
		deactivateBlackScreen();
		resetPrayerRowsStyleFunction();
		}
	}
	else
	{
		
		if(currentTimeInMinutes == dohrTimeInMinutes) startIqamaCounterFunction(JS_DATA.ucIqamaDOHR, JS_DATA.ucPrayDurationDOHR, 0);
	}

	

	if(currentTimeInMinutes == (JS_DATA.ucIqamaFAJR + fajrTimeInMinutes))
	{
		playIqamaSoundFunction();
	}

	if(currentTimeInMinutes == (JS_DATA.ucIqamaSHRQ + shuruqTimeInMinutes))
	{
		playDohaAlertSoundFunction();
	}


	if(currentTimeInMinutes == (JS_DATA.ucIqamaDOHR + dohrTimeInMinutes))
	{
		if(!isFriday) playIqamaSoundFunction();
	}


	if(currentTimeInMinutes == (JS_DATA.ucIqamaASSR + asrTimeInMinutes))
	{
		playIqamaSoundFunction();
	}


	if(currentTimeInMinutes == (JS_DATA.ucIqamaMGRB + maghribTimeInMinutes))
	{
		playIqamaSoundFunction();
	}


	if(currentTimeInMinutes == (JS_DATA.ucIqamaISHA + ishaTimeInMinutes))
	{
		playIqamaSoundFunction();
	}

	

	if((JS_DATA.ucPrimaryAzanMinutes > 0)&&(currentTimeInMinutes == (fajrTimeInMinutes - JS_DATA.ucPrimaryAzanMinutes)))
	{
		if(isTawkitApp) redirectToAudioHandlerFunction(audioAlert1ElementVar.dataset.xfile); else audioAlert1ElementVar.play();
	}	
	
	let isFridayAndDohr = ((currentTimeInMinutes == dohrTimeInMinutes) && (isFriday))


	 if((currentTimeInMinutes == fajrTimeInMinutes) ||
		(currentTimeInMinutes == shuruqTimeInMinutes) ||
		(currentTimeInMinutes == dohrTimeInMinutes) ||
		(currentTimeInMinutes == asrTimeInMinutes) ||
		(currentTimeInMinutes == maghribTimeInMinutes) ||
		(currentTimeInMinutes == ishaTimeInMinutes)
	)
	{

		if(currentTimeInMinutes != shuruqTimeInMinutes)
		{
			if(!isFridayAndDohr) playAzanSoundFunction();
			else
				{ 
				if(JS_DATA.ucActivateJomoaAzan == 1) playAzanSoundFunction();
				}
		}

		highlightCurrentPrayer(currentTimeInMinutes);

		
		if((!isRamadan) && (!isEidAlAdhaPeriod) && (!isLastDaysRamadan))
		{
			
			if(currentDayIndex == 0)
			{
				if((currentTimeInMinutes == maghribTimeInMinutes) || (currentTimeInMinutes == ishaTimeInMinutes))
					setTimeout(showMessagePopupFunction, (7 * 60000), fastingMondayMessage, 30);
			}

			
			if(currentDayIndex == 3)
			{
				if((currentTimeInMinutes == maghribTimeInMinutes) || (currentTimeInMinutes == ishaTimeInMinutes)) 
					setTimeout(showMessagePopupFunction, (7 * 60000), fastingThursdayMessage, 30);
			}
		}

	}


	if(timeString24 == "00:00")
	{
		midnightCounter++;
		calculateAndDisplayTimesFunction();
		updateBottomMessages();
		
		     if(JS_DATA.ucThemesActiveType == 2) changeThemeByDay();
		else if(JS_DATA.ucThemesActiveType == 3) changeThemeRandomly();
	}


	if(timeString24 == "00:01") updateWeatherData();
	
	

	updateNextPrayerDisplayFunction(currentTimeInMinutes);
	applyDimPastPrayers(currentTimeInMinutes);
	
	
	
	if((currentMinutes == '29')||(currentMinutes == '59'))
	{
	checkInternetConnectionFunction();
	}


	if(currentMinutes == '00')
	{
	refreshWeatherDisplay();
	}

	
	if(JS_DATA.ucThemesActiveType == 1)
	{
		if(isThemeChangeBySalatActive) 
		changeThemeByPrayer(currentTimeInMinutes);
	}

			if(JS_DATA.ucRemindersActive == 1)
			{
			checkReminders(currentTimeInMinutes);
			}

}






function countOccurrences(charToCount, sourceString)
{
return sourceString.split(charToCount).length - 1;
}




	

function checkReminders(currentTimeMinutes)
{

if( (midnightTimeInMinutes == 0) || (lastThirdTimeInMinutes == 0) ) return;


let reminderLine = "";
let reminderParts = "";
let reminderMinutes,reminderType,reminderPrayer,reminderMessage = "";



		for(let i=0; i < JS_REMINDERS_DATA.length; i++)
		{
		reminderLine = JS_REMINDERS_DATA[i];
			
			if(countOccurrences('.', reminderLine) > 2) 
			{
			reminderParts = reminderLine.split(".");
			
				reminderMinutes 		= parseInt(reminderParts[0]);
				reminderType 	= reminderParts[1];
				reminderPrayer 	= reminderParts[2];
				reminderMessage	= reminderParts[3];

				if(currentPrayerInterval.indexOf(reminderPrayer) < 0) continue; 
				

					

					if( (reminderType == "BEFORE") && (currentPrayerInterval.indexOf(reminderPrayer) > 0) )
					{
						let prayerPair 		= currentPrayerInterval.split('|');
						let nextPrayerName 	= prayerPair[1];
						let nextPrayerMinutes 	= prayerTimesMinutesObject[nextPrayerName];
						
							if( (currentTimeMinutes == (nextPrayerMinutes - reminderMinutes) ) )
							{
								testReminderSoundFunction();
								let extractedMessage = extractBetween(reminderMessage, "(", ")");
								if(extractedMessage !== "") doAlert(extractedMessage);
								break;
							}

					}


					if( (reminderType == "AFTER") && (currentPrayerInterval.indexOf(reminderPrayer) == 0) )
					{
						let prayerPair 		= currentPrayerInterval.split('|');
						let currentPrayerName 	= prayerPair[0];
						let nextPrayerMinutes 	= prayerTimesMinutesObject[currentPrayerName];
						
							if( (currentTimeMinutes == (nextPrayerMinutes + reminderMinutes) ) )
							{
								testReminderSoundFunction();
								let extractedMessage = extractBetween(reminderMessage, "(", ")");
								if(extractedMessage !== "") doAlert(extractedMessage);
								break;
							}

					}					

			}
		}
	
}





function setInternetStatus(isInternetConnected)
{
	if(isInternetConnected)	internetStatusHorizontalElement.style.color = '#00C110'; else 
						internetStatusHorizontalElement.style.color = '#DA0000';
						internetStatusVerticalElement.style.color = internetStatusHorizontalElement.style.color;
}



function checkInternetConnectionFunction()
{
if(JS_DATA.ucVerifyInternet == 0) return;

	if(window.navigator.onLine)
	{
		try {
			fetch(versionCheckUrl,{method:'HEAD'})
			.then(
					function(response)
					{
					setInternetStatus(response.ok);
					}
				)
			}
		catch {dolog('error_catch_fetch'); setInternetStatus(false);}
	}
	else
	{
	dolog('___navigator_NOT_onLine');
	setInternetStatus(false);
	}
}



function minutesToTimeString(minutes)
{
	let hoursPart   = Math.floor(minutes / 60);
	let minutesPart = minutes % 60;
	
	minutesPart = ('0' + minutesPart).slice(-2);
	return hoursPart + ':' + minutesPart;
}


function formatPrayerTimeDiff(minutes)
{
	let hoursPart   = Math.floor(minutes / 60);
	let minutesPart = minutes % 60;


	if((hoursPart == 0) && (minutesPart == 0))
	{
		nextPrayerNameTemp = '';
		if(currentTimeInMinutes == shuruqTimeInMinutes) return '&#9788;'; else return '-';
	}
	
	minutesPart = ('0' + minutesPart).slice(-2);
	return hoursPart + ':' + minutesPart;
}


let nextPrayerNameTemp = '-';

function updateNextPrayerDisplay(remainingMinutes)
{

let formattedRemainingTime = convertToArabicDigitsFunction(formatPrayerTimeDiff(remainingMinutes));

	nextPrayerTimeDisplayHorizontalElement.innerHTML = formattedRemainingTime;
	nextPrayerTimeDisplayVerticalElement.innerHTML = formattedRemainingTime;


	let nextPrayerLabelText = currentPrayerName; 
	if(nextPrayerNameTemp !== "")
	{
		if(JS_DATA.ucLangNOW == "AR")
		{
			nextPrayerLabelText = JS_eLang.cx_NextPrayTime + nextPrayerNameTemp;
			nextPrayerLabelText = nextPrayerLabelText.replace('لا', 'ل');
		}
		else nextPrayerLabelText = JS_eLang.cx_NextPrayTime;
	}


nextPrayerLabelHorizontalElement.innerHTML = nextPrayerLabelText;
nextPrayerLabelVerticalElement.innerHTML = nextPrayerLabelText;
}




const nextPrayerTimeDisplayHorizontalElement = document.getElementById('nextPrayerTimeDisplayHorizontal');
const nextPrayerTimeDisplayVerticalElement = document.getElementById('nextPrayerTimeDisplayVertical');
const jomoaTimeDisplayHorizontalElement = document.getElementById('jomoaTimeDisplayHorizontal');




let jomoaLabel = "---";

let isTawkitInfoMissing = false;


function updateMosqueAndDateDisplayFunction()
{

	let separator = "&nbsp; | &nbsp;";
	if(cityCodeDisplay == "&nbsp;") separator = "";

	let formattedJomoaTime = convertToArabicDigitsFunction(convertTo12HourFormat(jomoaFixedTime));

	jomoaTimeDisplayHorizontalElement.innerHTML = formattedJomoaTime;

	versionDisplayElement.innerHTML = appVersionString + separator + cityCodeDisplay;
	tawkitNameHorizontalElement.innerHTML = appVersionString;
	cityCodeHorizontalElement.innerHTML = cityCodeDisplay;
	aboutAppTitleElement.innerHTML = appVersionString;

}




function updateNextPrayerDisplayFunction(currentTimeMinutesLocal)
{

	if(currentTimeMinutesLocal <= fajrTimeInMinutes)
	{
					if(currentTimeMinutesLocal <= lastThirdTimeInMinutes) currentPrayerInterval = "NISF|TULT"; else currentPrayerInterval = "TULT|FAJR";
		nextPrayerNameTemp = prayerNamesArray[0];
		updateNextPrayerDisplay(fajrTimeInMinutes - currentTimeMinutesLocal);
		return;
	}

	if(currentTimeMinutesLocal <= shuruqTimeInMinutes)
	{
					currentPrayerInterval = "FAJR|SHRQ";
		nextPrayerNameTemp = prayerNamesArray[1];
		updateNextPrayerDisplay(shuruqTimeInMinutes - currentTimeMinutesLocal);
		return;
	}

	if(currentTimeMinutesLocal <= dohrTimeInMinutes)
	{
					currentPrayerInterval = "SHRQ|DOHR";
		nextPrayerNameTemp = prayerNamesArray[2];
		if(isFriday) nextPrayerNameTemp = jomoaLabel;
		updateNextPrayerDisplay(dohrTimeInMinutes - currentTimeMinutesLocal);
		return;
	}

	if(currentTimeMinutesLocal <= asrTimeInMinutes)
	{
					currentPrayerInterval = "DOHR|ASSR";
		nextPrayerNameTemp = prayerNamesArray[3];
		updateNextPrayerDisplay(asrTimeInMinutes - currentTimeMinutesLocal);
		return;
	}

	if(currentTimeMinutesLocal <= maghribTimeInMinutes)
	{
					currentPrayerInterval = "ASSR|MGRB";
		nextPrayerNameTemp = prayerNamesArray[4];
		updateNextPrayerDisplay(maghribTimeInMinutes - currentTimeMinutesLocal);
		return;
	}

	if(currentTimeMinutesLocal <= ishaTimeInMinutes)
	{
					currentPrayerInterval = "MGRB|ISHA";
		nextPrayerNameTemp = prayerNamesArray[5];
		updateNextPrayerDisplay(ishaTimeInMinutes - currentTimeMinutesLocal);
		return;
	}

	if(currentTimeMinutesLocal > ishaTimeInMinutes)
	{

					let midnightAdjusted = midnightTimeInMinutes;
					
					if(midnightTimeInMinutes < fajrTimeInMinutes)
					{
					midnightAdjusted = (1440 + midnightTimeInMinutes);
					}


					if(currentTimeMinutesLocal < midnightAdjusted) currentPrayerInterval = "ISHA|NISF"; else currentPrayerInterval = "NISF|TULT";
						
		nextPrayerNameTemp = prayerNamesArray[0];
		updateNextPrayerDisplay((1440 - currentTimeMinutesLocal) + fajrTimeInMinutes);
		return;
	}




}




function applyDimPastPrayers(currentTimeMinutesLocal)
{

	if(currentTimeMinutesLocal >= (maghribTimeInMinutes + 10))
	{
		nightPrayersContainerVerticalElement.style.opacity = '1';
		nightPrayersContainerHorizontalElement.style.opacity = '1';
	}
	else
	{
		nightPrayersContainerVerticalElement.style.opacity = '0.25';
		nightPrayersContainerHorizontalElement.style.opacity = '0.25';
	}

	prayerRowFajrVerticalElement.style.opacity = '1';
	prayerRowShrqVerticalElement.style.opacity = '1';
	prayerRowDohrVerticalElement.style.opacity = '1';
	prayerRowAsrVerticalElement.style.opacity = '1';
	prayerRowMgrbVerticalElement.style.opacity = '1';
	prayerRowIshaVerticalElement.style.opacity = '1';

	prayerCellFajrHorizontalElement.style.opacity = '1';
	prayerCellShrqHorizontalElement.style.opacity = '1';
	prayerCellDohrHorizontalElement.style.opacity = '1';
	prayerCellAsrHorizontalElement.style.opacity = '1';
	prayerCellMgrbHorizontalElement.style.opacity = '1';
	prayerCellIshaHorizontalElement.style.opacity = '1';

	if(JS_DATA.ucDimmPastPrayers == 0) return;

	if(currentTimeMinutesLocal > (fajrTimeInMinutes + 60))
	{
		prayerRowFajrVerticalElement.style.opacity = '0.3';
		prayerCellFajrHorizontalElement.style.opacity = '0.3';
	}
	if(currentTimeMinutesLocal > (shuruqTimeInMinutes + 30))
	{
		prayerRowShrqVerticalElement.style.opacity = '0.3';
		prayerCellShrqHorizontalElement.style.opacity = '0.3';
	}
	if(currentTimeMinutesLocal > (dohrTimeInMinutes + 40))
	{
		prayerRowDohrVerticalElement.style.opacity = '0.3';
		prayerCellDohrHorizontalElement.style.opacity = '0.3';
	}
	if(currentTimeMinutesLocal > (asrTimeInMinutes + 40))
	{
		prayerRowAsrVerticalElement.style.opacity = '0.3';
		prayerCellAsrHorizontalElement.style.opacity = '0.3';
	}
	if(currentTimeMinutesLocal > (maghribTimeInMinutes + 30))
	{
		prayerRowMgrbVerticalElement.style.opacity = '0.3';
		prayerCellMgrbHorizontalElement.style.opacity = '0.3';
	}
	if(currentTimeMinutesLocal > (ishaTimeInMinutes + 30))
	{
		prayerRowIshaVerticalElement.style.opacity = '0.3';
		prayerCellIshaHorizontalElement.style.opacity = '0.3';
	}

}



function changeThemeByPrayer(currentTimeMinutesLocal)
{
let themeIndexForPrayer = -1;

		 if(currentTimeMinutesLocal <  fajrTimeInMinutes - JS_Minutes_BeforeAZAN_ToChange_Theme) themeIndexForPrayer =  5;
	else if(currentTimeMinutesLocal <  shuruqTimeInMinutes - JS_Minutes_BeforeAZAN_ToChange_Theme) themeIndexForPrayer =  0;
	else if(currentTimeMinutesLocal <  dohrTimeInMinutes - JS_Minutes_BeforeAZAN_ToChange_Theme) themeIndexForPrayer =  1;
	else if(currentTimeMinutesLocal <  asrTimeInMinutes - JS_Minutes_BeforeAZAN_ToChange_Theme) themeIndexForPrayer =  2;
	else if(currentTimeMinutesLocal <  maghribTimeInMinutes - JS_Minutes_BeforeAZAN_ToChange_Theme) themeIndexForPrayer =  3;
	else if(currentTimeMinutesLocal <  ishaTimeInMinutes - JS_Minutes_BeforeAZAN_ToChange_Theme) themeIndexForPrayer =  4;
	else if(currentTimeMinutesLocal >= ishaTimeInMinutes - JS_Minutes_BeforeAZAN_ToChange_Theme) themeIndexForPrayer =  5;



currentThemeIndexForPrayer = themeIndexForPrayer;

	let themeIndexFromSettings = parseInt(themesForEachSalat[currentThemeIndexForPrayer]);
	if(!Number.isInteger(themeIndexFromSettings)) return;
	
	if(themeIndexFromSettings !== currentThemeIndex)
	{
	currentThemeIndex = themeIndexFromSettings; 
	setThemeFunction(currentThemeIndex, true);
	}
}






function highlightPrayerRow(verticalRowElement, horizontalCellElement, isShuruqPrayer)
{

	if(isShuruqPrayer == 1)
	{
	showAzanPopup(true);
	return;
	}
	
	showAzanPopup(false);
	setTimeout(positionHighlightBar, 1000, verticalRowElement, horizontalCellElement);
}




function getOffset(el)
{
	const rect = el.getBoundingClientRect();
	return {
		left: rect.left,
		top: rect.top,
		width: rect.width,
		height: rect.height
	};
}



const highlightBarVerticalElement = document.getElementById('highlightBarVertical');
const highlightBarHorizontalElement = document.getElementById('highlightBarHorizontal');




function positionHighlightBar(verticalElement, horizontalElement)
{
	if(JS_DATA.ucIsForHome == 1) return; 


let highlightOffset = 8;
if(JS_DATA.ucHr5BoxesOnly == 1) highlightOffset = 18;


	
	let elementRect = getOffset(horizontalElement);
	

	let elementLeft = parseInt(elementRect.left);
	let elementTop = parseInt(elementRect.top);

	let cellWidthPart = Math.floor(elementRect.width / 6);
	let cellWidthHalf = Math.floor(cellWidthPart / 2);

	


	
	highlightBarHorizontalElement.style.width = elementRect.width + cellWidthPart + 'px';
	highlightBarHorizontalElement.style.height = highlightBarHorizontalElement.style.width;

	highlightBarHorizontalElement.style.left = (elementLeft - cellWidthHalf) + 'px';
	highlightBarHorizontalElement.style.top = (elementTop - cellWidthHalf - highlightOffset) + 'px';
	horizontalElement.className = 'highlightedCellClass';

	showElementFunction('highlightBarHorizontal');
}



function highlightCurrentPrayer(prayerTimeMinutes)
{




	if(prayerTimeMinutes == fajrTimeInMinutes)
	{
		currentPrayerIndex = 0;
		currentPrayerName = prayerNamesArray[0];
		highlightPrayerRow(prayerRowFajrVerticalElement, prayerCellFajrHorizontalElement, 0);
		return;
	}
	if(prayerTimeMinutes == shuruqTimeInMinutes)
	{
		currentPrayerIndex = 1;
		currentPrayerName = prayerNamesArray[1];
		highlightPrayerRow(prayerRowShrqVerticalElement, prayerCellShrqHorizontalElement, 1);
		return;
	}
	if(prayerTimeMinutes == dohrTimeInMinutes)
	{
		currentPrayerIndex = 2;
		currentPrayerName = prayerNamesArray[2];
		highlightPrayerRow(prayerRowDohrVerticalElement, prayerCellDohrHorizontalElement, 2);
		return;
	}
	if(prayerTimeMinutes == asrTimeInMinutes)
	{
		currentPrayerIndex = 3;
		currentPrayerName = prayerNamesArray[3];
		highlightPrayerRow(prayerRowAsrVerticalElement, prayerCellAsrHorizontalElement, 3);
		return;
	}
	if(prayerTimeMinutes == maghribTimeInMinutes)
	{
		currentPrayerIndex = 4;
		currentPrayerName = prayerNamesArray[4];
		highlightPrayerRow(prayerRowMgrbVerticalElement, prayerCellMgrbHorizontalElement, 4);
		return;
	}
	if(prayerTimeMinutes == ishaTimeInMinutes)
	{
		currentPrayerIndex = 5;
		currentPrayerName = prayerNamesArray[5];
		highlightPrayerRow(prayerRowIshaVerticalElement, prayerCellIshaHorizontalElement, 5);
		return;
	}
}






const azanPopupPrayerNameVerticalElement = document.getElementById('azanPopupPrayerNameVertical');
const azanPopupTimeVerticalElement = document.getElementById('azanPopupTimeVertical');
const azanPopupTimeHorizontalElement = document.getElementById('azanPopupTimeHorizontal');
const azanPopupPrayerNameHorizontalElement = document.getElementById('azanPopupPrayerNameHorizontal');

let isAzanPopupVisible = false;
function showAzanPopup(isDohaPrayer)
{

let azanPopupDisplayDuration = (JS_AZAN_WINDOW_SHOW_TIME * 1000);

		if(isDohaPrayer)
		{
		azanPopupDisplayDuration = 10000;
		azanPopupTitleVerticalElement.innerHTML = "&nbsp;";
		azanPopupTitleHorizontalElement.innerHTML = "&nbsp;";
		azanPopupPrayerNameVerticalElement.innerHTML = JS_eLang.cx_WQT_SHRQ;
		azanPopupPrayerNameHorizontalElement.innerHTML = JS_eLang.cx_WQT_SHRQ;
		}
	else
		{
		azanPopupTitleVerticalElement.innerHTML = JS_eLang.cx_AzanCalling;
		azanPopupTitleHorizontalElement.innerHTML = JS_eLang.cx_AzanCalling;
		azanPopupPrayerNameVerticalElement.innerHTML = currentPrayerName;
		azanPopupPrayerNameHorizontalElement.innerHTML = currentPrayerName;
		}

	if((isFriday) && (currentTimeInMinutes == dohrTimeInMinutes))
	{
	azanPopupPrayerNameVerticalElement.innerHTML = jomoaLabel;
	azanPopupPrayerNameHorizontalElement.innerHTML = jomoaLabel;
	}



	if( (!isDohaPrayer) && (JS_DATA.ucLangNOW == "AR") ) setTimeout(showMessagePopupFunction, 50000, JS_DuaAfterAZAN, 1);

	if(isIqamaCounterActive) showIqamaCounter(); 
	if(JS_DATA.ucShowAzanWindow == 0) return;
	
	showElementWithTransitionFunction('azanPopupVertical');
	showElementWithTransitionFunction('azanPopupHorizontal');
	isAzanPopupVisible = true;
	setTimeout('hideAzanPopupFunction()', azanPopupDisplayDuration);
	

}




function hideAzanPopupFunction()
{
	isAzanPopupVisible = false;
	hideElementFunction('azanPopupVertical');
	hideElementFunction('azanPopupHorizontal');
}




function showElementWithTransitionFunction(elementId)
{
	document.getElementById(elementId).className = "visibleTransitionClass";
}
function hideElementFunction(elementId)
{
	document.getElementById(elementId).className = "hiddenClass";
}

function showSemiTransparentElementFunction(elementId)
{
	document.getElementById(elementId).className = "semiTransparentClass";
}


function showFullScreenElementFunction(elementId)
{
	document.getElementById(elementId).className = "fullScreenVisibleClass";
}

function hideFullScreenElementFunction(elementId)
{
	document.getElementById(elementId).className = "fullScreenHiddenClass";
}



function fadeInElementFunction(elementId)
{
	document.getElementById(elementId).className = "fadeInClass";
}

function fadeOutElementFunction(elementId)
{
	document.getElementById(elementId).className = "fadeOutClass";
}

let isFullScreenCounterMode = false;


function showIqamaCounter()
{

	if((!JS_DATA.ucIqamaCounter == 1) || (JS_DATA.ucIsForHome == 1)) return;

	let isFullScreenCounterForced =  false;

	if((iqamaMinutesParam >= 1)&&(JS_DATA.ucIqamaCounter == 1))
	{
			
			if((JS_DATA.ucFullScreenCounter == 1)&&(!isFullScreenCounterForced))
			{
			isFullScreenCounterMode = true;
			ayaContainerVertical.style.top = '11%';
			ayaContainerVertical.style.zIndex = 1000;
			messageContainerVertical.style.top = '11%';
			messageContainerVertical.style.zIndex = 1000;
			hadithDisplayHorizontalElement.style.zIndex = 1000;
			marqueeContainerHorizontalElement.style.zIndex = 1000;
			staticMessageHorizontalElement.style.zIndex = 999;
			showElementWithTransitionFunction('fullScreenCounterContainerVertical');
			showElementWithTransitionFunction('fullScreenCounterContainerHorizontal');
			updateTimeAndPrayersFunction(true); 
			rootElement.style.setProperty('--izBigCNTR', 0);
			showPrayerTimesOverlayFunction(true);
			}
			else
				{
				isFullScreenCounterMode = false;
				showElementWithTransitionFunction('iqamaCounterContainerVertical');
				showElementWithTransitionFunction('iqamaCounterContainerHorizontal');
				}
	}

}





function resetPrayerRowsStyleFunction()
{
	prayerRowFajrVerticalElement.className = 'prayerRowClass';
	prayerRowShrqVerticalElement.className = 'prayerRowSecondaryClass';
	prayerRowDohrVerticalElement.className = 'prayerRowClass';
	prayerRowAsrVerticalElement.className = 'prayerRowClass';
	prayerRowMgrbVerticalElement.className = 'prayerRowClass';
	prayerRowIshaVerticalElement.className = 'prayerRowClass';

	prayerCellFajrHorizontalElement.className = 'prayerCellClass';
	prayerCellShrqHorizontalElement.className = 'prayerCellClass';
	prayerCellDohrHorizontalElement.className = 'prayerCellClass';
	prayerCellAsrHorizontalElement.className = 'prayerCellClass';
	prayerCellMgrbHorizontalElement.className = 'prayerCellClass';
	prayerCellIshaHorizontalElement.className = 'prayerCellClass';

	hideElementByIdFunction('highlightBarVertical');
	hideElementByIdFunction('highlightBarHorizontal');
}




function addOneHour(timeString)
{
	let timeParts = timeString.split(':');
	let adjustedHours = parseInt(timeParts[0]);

	adjustedHours++;

	adjustedHours = '0' + adjustedHours;
	adjustedHours = adjustedHours.slice(-2);

	return adjustedHours + ':' + timeParts[1];
}




function subtractOneHour(timeString)
{

	let timeParts = timeString.split(':');
	let adjustedHours = parseInt(timeParts[0]);

	adjustedHours--;

	adjustedHours = '0' + adjustedHours;
	adjustedHours = adjustedHours.slice(-2);

	return adjustedHours + ':' + timeParts[1];
}





function findIndexInArray(selectedLanguageCode, targetArray)
{
    if (!Array.isArray(targetArray)) return -1;  

    for (let i = 0; i < targetArray.length; i++) {
        if (typeof targetArray[i] === "string" && targetArray[i].match(selectedLanguageCode)) {
            return i;
        }
    }
    return -1;
}



function changeScreenFontFunction(elementIdParam)
{
	JS_DATA.ucScreenFont = elementIdParam;
	saveSettingsToStorageFunction();
	rootElement.style.setProperty('--csvar_mainFONT', JS_DATA.ucScreenFont);
}




function changeTimesFontFunction(elementIdParam)
{
	JS_DATA.ucTimesFont = elementIdParam;
	saveSettingsToStorageFunction();
	applyArabicDigits(JS_DATA.ucIsArabicDigits,true);
}




function changeClockFontFunction(elementIdParam)
{
	JS_DATA.ucClockFont = elementIdParam;
	saveSettingsToStorageFunction();
	applyArabicDigits(JS_DATA.ucIsArabicDigits,true);
}




function changeAzkarFontFunction(elementIdParam)
{
	JS_DATA.ucAzkarFont = elementIdParam;
	saveSettingsToStorageFunction();
	rootElement.style.setProperty('--csvar_azkarFONT', JS_DATA.ucAzkarFont);
	
	adjustAzkarFontSize();
}


function adjustAzkarFontSize()
{
let sultanFontOffset = '0'+fontSizeUnit;
if(JS_DATA.ucAzkarFont  == 'SULTAN') sultanFontOffset = '0.7'+fontSizeUnit;
rootElement.style.setProperty('--NowSULTAN', sultanFontOffset);
}


function convertTo12HourFormat(time24h)
{
	if(JS_DATA.ucActivate24Hours == 1)
	{
		if(time24h.length < 5) time24h = '0' + time24h;
		return time24h;
	}

	let timeParts = time24h.split(':');
	let hoursForAmPm, minutesFormatted, timeStringAmPm, unusedVariable;

	hoursForAmPm = parseInt(timeParts[0]);
	minutesFormatted = timeParts[1];

	if(hoursForAmPm >= 12)
	{
		if(hoursForAmPm > 12) hoursForAmPm = hoursForAmPm - 12;
	}
	else
	{
		if(hoursForAmPm == 0) hoursForAmPm = 12;

	}

	let time12hFormatted = hoursForAmPm + ':' + minutesFormatted;

	if(JS_DATA.ucAddZeroToAMPM == 1)
	{
		if(time12hFormatted.length < 5) time12hFormatted = '0' + time12hFormatted;
	}

	return time12hFormatted;
}

let isSummerTimeActive = false;
let cityCodeDisplay = '';
let hijriDateString = '-----';
let hijriDateShort = '-----';
let hijriDateArray = ['-', '-', '-'];
let isEidAlAdhaPeriod = false;
let isLastDaysRamadan = false;
let isFirstTenDhulHijjah = false;
let isEidOrTashreeq = false;
let isLastTenRamadan = false;
let isWhiteDaysPeriod = false;
let isDhulHijjahFirstNine = false;
let isEidFitrPeriod = false;
let isEidAdhaPeriod = false;

let prayerHourPrefixes = ['0', '0', '0', '0', '0', '0'];
let prayerTimesMinutesArray = [0, 0, 0, 0, 0, 0];

let weekDaysNamesEnglish = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "JOMOA", "SATURDAY"];
let currentWeekDayName = "";
let hijriMonthDay = "";
let specialEventLabel = "";

let currentDayName = "";
let gregorianDateHtml = "";
let hijriDateDisplay = "";
let cityCodeDisplayString = "";



function calculateAndDisplayTimesFunction()
{


	let currentDateObject = new Date();
	

	if( (JS_DATA.ucLocalHoursAdjustment !== 0) && (allowedHourAdjustments.indexOf(JS_DATA.ucLocalHoursAdjustment) !== -1) )
	{currentDateObject.setTime(currentDateObject.getTime() + (JS_DATA.ucLocalHoursAdjustment*60*60*1000));}



	MiladiToHIJRI(currentDateObject, JS_DATA.ucHijriDateFixer);

	let currentDayIndex = currentDateObject.getDay();
	currentDayName = weekDayNamesArray[currentDayIndex];
	currentWeekDayName = weekDaysNamesEnglish[currentDayIndex];


	isFriday = (currentDayIndex == 5);

	let currentYear = currentDateObject.getFullYear();



	let currentMonth = (currentDateObject.getMonth());
	let currentDay = currentDateObject.getDate();
	let currentDatePadded = '0' + currentDay;
	currentDatePadded = currentDatePadded.slice(-2);

	let currentMonthPadded = '0' + (currentMonth + 1);
	currentMonthPadded = currentMonthPadded.slice(-2);
	
	currentDateString = currentDay+'-'+(currentMonth+1)+'-'+currentYear;

	let fajrTimeStr = '-';
	let shuruqTimeStr = '-';
	let dohrTimeStr = '-';
	let asrTimeStr = '-';
	let maghribTimeStr = '-';
	let ishaTimeStr = '-';


	secondJomoaDisplayHorizontal.innerHTML = '';

	if(JS_DATA.ucWcsvIsActive == 1)
	{


		let dateKey = currentMonthPadded + '-' + currentDatePadded;
		let dateIndex = findIndexInArray(dateKey, JS_WCSV_DATA);
		if(dateIndex == -1)
		{
			dolog('WCSV : Error in your file (wcsv.js), date not found : ' + dateKey);
			return;
		}

		let wcsvLine = JS_WCSV_DATA[dateIndex];
		let wcsvFields = wcsvLine.split(',');

		fajrTimeStr = wcsvFields[2];
		shuruqTimeStr = wcsvFields[4];
		dohrTimeStr = wcsvFields[5];
		asrTimeStr = wcsvFields[7];
		maghribTimeStr = wcsvFields[9];
		ishaTimeStr = wcsvFields[11];

		firstJomoaTime = wcsvFields[13]; 
		secondJomoaTime = wcsvFields[14]; 

		JS_DATA.ucIqamaFAJR = timeStringToMinutesFunction(wcsvFields[3]) - timeStringToMinutesFunction(fajrTimeStr);
		JS_DATA.ucIqamaDOHR = timeStringToMinutesFunction(wcsvFields[6]) - timeStringToMinutesFunction(dohrTimeStr);
		JS_DATA.ucIqamaASSR = timeStringToMinutesFunction(wcsvFields[8]) - timeStringToMinutesFunction(asrTimeStr);
		JS_DATA.ucIqamaMGRB = timeStringToMinutesFunction(wcsvFields[10]) - timeStringToMinutesFunction(maghribTimeStr);
		JS_DATA.ucIqamaISHA = timeStringToMinutesFunction(wcsvFields[12]) - timeStringToMinutesFunction(ishaTimeStr);

		cityCodeDisplay = "&nbsp;";
	}
	else
	{

		let dateKeyForTimes = currentMonthPadded + '-' + currentDatePadded;
		let dateIndex = findIndexInArray(dateKeyForTimes, JS_TIMES);
		if(dateIndex == -1)
		{
			if(dateKeyForTimes == "02-29")
				dateIndex = findIndexInArray("03-01", JS_TIMES); 
			else
			{
				dolog('Error in file (wtimes), date not found: ' + dateKeyForTimes);
				return;
			}
		}

		let timesDataLine = JS_TIMES[dateIndex];
		let timesDataParts = timesDataLine.split('~~~~~');
		let prayerTimesArray = timesDataParts[1].split('|');

		fajrTimeStr = prayerTimesArray[0];
		shuruqTimeStr = prayerTimesArray[1];
		dohrTimeStr = prayerTimesArray[2];
		asrTimeStr = prayerTimesArray[3];
		maghribTimeStr = prayerTimesArray[4];
		ishaTimeStr = prayerTimesArray[5];

		cityCodeDisplay = '&nbsp;'+JS_DATA.ucNowCityCODE;
		

				fajrTimeStr = adjustAthanTime(fajrTimeStr, JS_DATA.ucAthanMinutesFAJR);
				shuruqTimeStr = adjustAthanTime(shuruqTimeStr, JS_DATA.ucAthanMinutesSHRQ);
				dohrTimeStr = adjustAthanTime(dohrTimeStr, JS_DATA.ucAthanMinutesDOHR);
				asrTimeStr = adjustAthanTime(asrTimeStr, JS_DATA.ucAthanMinutesASSR);
				maghribTimeStr = adjustAthanTime(maghribTimeStr, JS_DATA.ucAthanMinutesMGRB);
				ishaTimeStr = adjustAthanTime(ishaTimeStr, JS_DATA.ucAthanMinutesISHA);	
	}



	isSummerTimeActive = false;

	if(JS_DATA.ucInSummerAdd1Hour == 1)
	{
		const summerMonthsArray = [3, 4, 5, 6, 7, 8, 9];

		if(summerMonthsArray.indexOf(currentMonth) !== -1)
		{
			fajrTimeStr = addOneHour(fajrTimeStr);
			shuruqTimeStr = addOneHour(shuruqTimeStr);
			dohrTimeStr = addOneHour(dohrTimeStr);
			asrTimeStr = addOneHour(asrTimeStr);
			maghribTimeStr = addOneHour(maghribTimeStr);
			ishaTimeStr = addOneHour(ishaTimeStr);
			isSummerTimeActive = true;
		}


		if(currentMonth == 2)
		{
			let JSDXXX = 24;
			let JSvDAT = null;
			let dayOfWeek = -1;

			for(i = 25; i <= 31; i++)
			{
				JSvDAT = new Date(currentYear, 2, i);
				dayOfWeek = JSvDAT.getDay();
				if(dayOfWeek == 0)
				{
					JSDXXX = i;
					break;
				}
			}

			if(currentDay >= JSDXXX)
			{
				fajrTimeStr = addOneHour(fajrTimeStr);
				shuruqTimeStr = addOneHour(shuruqTimeStr);
				dohrTimeStr = addOneHour(dohrTimeStr);
				asrTimeStr = addOneHour(asrTimeStr);
				maghribTimeStr = addOneHour(maghribTimeStr);
				ishaTimeStr = addOneHour(ishaTimeStr);
				isSummerTimeActive = true;
			}
		}


		if(currentMonth == 9)
		{
			let JSDXXX = 24;
			let JSvDAT = null;
			let dayOfWeek = -1;

			for(i = 25; i <= 31; i++)
			{
				JSvDAT = new Date(currentYear, 9, i);
				dayOfWeek = JSvDAT.getDay();
				if(dayOfWeek == 0)
				{
					JSDXXX = i;
					break;
				}
			}

			if(currentDay >= JSDXXX)
			{
				fajrTimeStr = subtractOneHour(fajrTimeStr);
				shuruqTimeStr = subtractOneHour(shuruqTimeStr);
				dohrTimeStr = subtractOneHour(dohrTimeStr);
				asrTimeStr = subtractOneHour(asrTimeStr);
				maghribTimeStr = subtractOneHour(maghribTimeStr);
				ishaTimeStr = subtractOneHour(ishaTimeStr);
				isSummerTimeActive = false;
			}
		}
	}


	if(JS_DATA.ucForce1HourMore == 1)
	{
		fajrTimeStr = addOneHour(fajrTimeStr);
		shuruqTimeStr = addOneHour(shuruqTimeStr);
		dohrTimeStr = addOneHour(dohrTimeStr);
		asrTimeStr = addOneHour(asrTimeStr);
		maghribTimeStr = addOneHour(maghribTimeStr);
		ishaTimeStr = addOneHour(ishaTimeStr);
	}


	if(JS_DATA.ucForce1HourLess == 1)
	{
		fajrTimeStr = subtractOneHour(fajrTimeStr);
		shuruqTimeStr = subtractOneHour(shuruqTimeStr);
		dohrTimeStr = subtractOneHour(dohrTimeStr);
		asrTimeStr = subtractOneHour(asrTimeStr);
		maghribTimeStr = subtractOneHour(maghribTimeStr);
		ishaTimeStr = subtractOneHour(ishaTimeStr);
	}


	if((isRamadan) && (JS_DATA.ucRamadanDoIsha30min == 1)) ishaTimeStr = adjustAthanTime(ishaTimeStr, 30);

	if(JS_DATA.ucJomoaFixedTime == 'AUTO')
	{
		jomoaFixedTime = getNextFridayPrayerTime(2);
	}


	if((JS_DATA.ucDohrXminutesAsr > 0) && (!JS_DATA.ucWcsvIsActive == 1))
	{
		dohrTimeStr = adjustAthanTime(asrTimeStr, -(JS_DATA.ucDohrXminutesAsr));

		if(JS_DATA.ucJomoaFixedTime == 'AUTO')
		{
			jomoaFixedTime = adjustAthanTime(getNextFridayPrayerTime(3), -(JS_DATA.ucDohrXminutesAsr));
		}
	}
	



	if(midnightCounter > 1)
	  {midnightCounter = 0;  decodedStringDisplayElement.innerHTML = decodeHexStringFunction(decodedStringVar);}
		
	if(isFriday)
	{
		
		prayerNameDohrVerticalSpanElement.innerHTML = jomoaLabel;
		prayerNameDohrHorizontalSpanElement.innerHTML = prayerNameDohrVerticalSpanElement.innerHTML;

		if(JS_DATA.ucJomoaFixedTime !== "")
		{

			if(JS_DATA.ucJomoaFixedTime == 'AUTO')
			{

				if(JS_DATA.ucWcsvIsActive == 1)
				{
					jomoaFixedTime = firstJomoaTime;
					secondJomoaDisplayHorizontal.innerHTML = '2nd Jomoa : ' + secondJomoaTime;
				}
				else
				{
					if(JS_DATA.ucDohrXminutesAsr > 0)
						jomoaFixedTime = adjustAthanTime(asrTimeStr, -(JS_DATA.ucDohrXminutesAsr));
					else
						jomoaFixedTime = dohrTimeStr;
				}
			}
			else
			{
				jomoaFixedTime = JS_DATA.ucJomoaFixedTime;
			}

			dohrTimeStr = jomoaFixedTime;
		}
	}
	else
	{
		prayerNameDohrVerticalSpanElement.innerHTML = prayerNamesArray[2];
		prayerNameDohrHorizontalSpanElement.innerHTML = prayerNameDohrVerticalSpanElement.innerHTML;
	}


	if((JS_DATA.ucFixedIqamaFAJR !== "") && (!JS_DATA.ucWcsvIsActive == 1))
	{
		JS_DATA.ucIqamaFAJR = timeStringToMinutesFunction(JS_DATA.ucFixedIqamaFAJR) - timeStringToMinutesFunction(fajrTimeStr);
		if(JS_DATA.ucIqamaFAJR < 1) JS_DATA.ucIqamaFAJR = 10;
	}


	if((JS_DATA.ucFixedIqamaDOHR !== "") && (!JS_DATA.ucWcsvIsActive == 1))
	{
		JS_DATA.ucIqamaDOHR = timeStringToMinutesFunction(JS_DATA.ucFixedIqamaDOHR) - timeStringToMinutesFunction(dohrTimeStr);
		if(JS_DATA.ucIqamaDOHR < 1) JS_DATA.ucIqamaDOHR = 10;
	}


	if((JS_DATA.ucFixedIqamaASSR !== "") && (!JS_DATA.ucWcsvIsActive == 1))
	{
		JS_DATA.ucIqamaASSR = timeStringToMinutesFunction(JS_DATA.ucFixedIqamaASSR) - timeStringToMinutesFunction(asrTimeStr);
		if(JS_DATA.ucIqamaASSR < 1) JS_DATA.ucIqamaASSR = 10;
	}


	if((JS_DATA.ucFixedIqamaISHA !== "") && (!JS_DATA.ucWcsvIsActive == 1))
	{
		JS_DATA.ucIqamaISHA = timeStringToMinutesFunction(JS_DATA.ucFixedIqamaISHA) - timeStringToMinutesFunction(ishaTimeStr);
		if(JS_DATA.ucIqamaISHA < 1) JS_DATA.ucIqamaISHA = 10; 
	}

	prayerHourPrefixes[0] = "T" + fajrTimeStr.split(":")[0] + ":00";
	prayerHourPrefixes[1] = "T" + shuruqTimeStr.split(":")[0] + ":00";
	prayerHourPrefixes[2] = "T" + dohrTimeStr.split(":")[0] + ":00";
	prayerHourPrefixes[3] = "T" + asrTimeStr.split(":")[0] + ":00";
	prayerHourPrefixes[4] = "T" + maghribTimeStr.split(":")[0] + ":00";
	prayerHourPrefixes[5] = "T" + ishaTimeStr.split(":")[0] + ":00";


	let azanTimeFormattedFajr = convertToArabicDigitsFunction(convertTo12HourFormat(fajrTimeStr));
	let azanTimeFormattedShuruq = convertToArabicDigitsFunction(convertTo12HourFormat(shuruqTimeStr));
	let azanTimeFormattedDohr = convertToArabicDigitsFunction(convertTo12HourFormat(dohrTimeStr));
	let azanTimeFormattedAsr = convertToArabicDigitsFunction(convertTo12HourFormat(asrTimeStr));
	let azanTimeFormattedMaghrib = convertToArabicDigitsFunction(convertTo12HourFormat(maghribTimeStr));
	let azanTimeFormattedIsha = convertToArabicDigitsFunction(convertTo12HourFormat(ishaTimeStr));
	azanTimeFajrVerticalSpanElement.innerHTML = azanTimeFormattedFajr;
	azanTimeShrqVerticalSpanElement.innerHTML = azanTimeFormattedShuruq;
	azanTimeDohrVerticalSpanElement.innerHTML = azanTimeFormattedDohr;
	azanTimeAsrVerticalSpanElement.innerHTML = azanTimeFormattedAsr;
	azanTimeMgrbVerticalSpanElement.innerHTML = azanTimeFormattedMaghrib;
	azanTimeIshaVerticalSpanElement.innerHTML = azanTimeFormattedIsha;
	

	aboutExtraInfoElement.innerHTML = decodeHexStringFunction(encodedString4);
	fajrTimeInMinutes = timeStringToMinutesFunction(fajrTimeStr);
	shuruqTimeInMinutes = timeStringToMinutesFunction(shuruqTimeStr);
	dohrTimeInMinutes = timeStringToMinutesFunction(dohrTimeStr);
	asrTimeInMinutes = timeStringToMinutesFunction(asrTimeStr);
	maghribTimeInMinutes = timeStringToMinutesFunction(maghribTimeStr);
	ishaTimeInMinutes = timeStringToMinutesFunction(ishaTimeStr);
	
	

prayerTimesMinutesObject.FAJR = fajrTimeInMinutes;
prayerTimesMinutesObject.SHRQ = shuruqTimeInMinutes;
prayerTimesMinutesObject.DOHR = dohrTimeInMinutes;
prayerTimesMinutesObject.ASSR = asrTimeInMinutes;
prayerTimesMinutesObject.MGRB = maghribTimeInMinutes;
prayerTimesMinutesObject.ISHA = ishaTimeInMinutes;


	
	azanTimeFajrHorizontalSpanElement.innerHTML = azanTimeFormattedFajr;
	azanTimeShrqHorizontalSpanElement.innerHTML = azanTimeFormattedShuruq;
	azanTimeDohrHorizontalSpanElement.innerHTML = azanTimeFormattedDohr;
	azanTimeAsrHorizontalSpanElement.innerHTML = azanTimeFormattedAsr;
	azanTimeMgrbHorizontalSpanElement.innerHTML = azanTimeFormattedMaghrib;
	azanTimeIshaHorizontalSpanElement.innerHTML = azanTimeFormattedIsha;

	prayerTimeFajrMiniHorizontalSpanElement.innerHTML = azanTimeFormattedFajr;
	prayerTimeShrqMiniHorizontalSpanElement.innerHTML = azanTimeFormattedShuruq;
	prayerTimeDohrMiniHorizontalSpanElement.innerHTML = azanTimeFormattedDohr;
	prayerTimeAsrMiniHorizontalSpanElement.innerHTML = azanTimeFormattedAsr;
	prayerTimeMgrbMiniHorizontalSpanElement.innerHTML = azanTimeFormattedMaghrib;
	prayerTimeIshaMiniHorizontalSpanElement.innerHTML = azanTimeFormattedIsha;

	prayerTimeFajrMiniVerticalSpanElement.innerHTML = azanTimeFormattedFajr;
	prayerTimeShrqMiniVerticalSpanElement.innerHTML = azanTimeFormattedShuruq;
	prayerTimeDohrMiniVerticalSpanElement.innerHTML = azanTimeFormattedDohr;
	prayerTimeAsrMiniVerticalSpanElement.innerHTML = azanTimeFormattedAsr;
	prayerTimeMgrbMiniVerticalSpanElement.innerHTML = azanTimeFormattedMaghrib;
	prayerTimeIshaMiniVerticalSpanElement.innerHTML = azanTimeFormattedIsha;


	    
	if(counterForDohaTimeDisplay > 3) checkForTawkitDisplayUpdate();

	prayerTimesMinutesArray[0] = fajrTimeInMinutes;
	prayerTimesMinutesArray[1] = shuruqTimeInMinutes;
	prayerTimesMinutesArray[2] = dohrTimeInMinutes;
	prayerTimesMinutesArray[3] = asrTimeInMinutes;
	prayerTimesMinutesArray[4] = maghribTimeInMinutes;
	prayerTimesMinutesArray[5] = ishaTimeInMinutes;

	let dirAttribute = "dir='ltr'";
	if(isRTL) dirAttribute = "dir='rtl'";

	let dateSeparatorHtml = "<span id='dateSeparatorId' " + dirAttribute + ">/</span>";
	let AnneeCourante = currentDateObject.getFullYear();

	hijriDateDisplay = convertToArabicDigitsFunction(hijriDateString);
	cityCodeDisplayString = convertToArabicDigitsFunction(hijriDateShort);
	aboutDescriptionElement.innerHTML = decodeHexStringFunction(encodedString3);


	gregorianDateHtml = "<span id='gregorianDateSpan'" + dirAttribute + ">" + "<span id='daySpan'>" + convertToArabicDigitsFunction(currentDatePadded) + "</span>" + dateSeparatorHtml + "<span id='gregorianMonthSpanId'>" + convertToArabicDigitsFunction(currentMonthPadded) + "</span>" + dateSeparatorHtml + convertToArabicDigitsFunction(AnneeCourante) + "</span>";
	let gregorianDateString = convertToArabicDigitsFunction(currentDatePadded) + dateSeparatorHtml + convertToArabicDigitsFunction(currentMonthPadded) + dateSeparatorHtml + convertToArabicDigitsFunction(AnneeCourante);
	cityCodeVerticalElement.innerHTML = gregorianDateString;

	
	dateDisplayVerticalElement.innerHTML = currentDayName + lineBreakOrSpace + cityCodeDisplayString;
	
	
	

	updateDateDisplay();
	updateMosqueAndDateDisplayFunction();
	updateIqamaTimesDisplayFunction();
	updateNextPrayerDisplayFunction(currentTimeInMinutes);
	updateEidMessageVisibility();
	
	menuVersionDisplayElement.innerHTML = appVersionString;



	calculateNightPrayersFunction();

}




function updateDateDisplay()
{
	dateTextSpanHorizontalElement.innerHTML = "<span id='weekDaySpan'>" +currentDayName + "</span>" + " " + hijriDateDisplay + " . " + gregorianDateHtml;
}




function adjustAthanTime(hexString, minutesOffset)
{

	let totalMinutes = timeStringToMinutesFunction(hexString);
	totalMinutes += minutesOffset;
	let adjustedHours = Math.floor(totalMinutes / 60);
	let adjustedMinutes = totalMinutes % 60;

	adjustedHours = '0' + adjustedHours;
	adjustedMinutes = '0' + adjustedMinutes;

	adjustedHours = adjustedHours.slice(-2);
	adjustedMinutes = adjustedMinutes.slice(-2);

	return(adjustedHours + ':' + adjustedMinutes);
}







function getNextFridayPrayerTime(prayerIndexForFriday)
{

	let tempDate = new Date;

	let tempMonth = tempDate.getMonth();
	let tempDay = tempDate.getDate();


	let dayOfWeek = -1;
	let fridayPrayerTime = "---";

	for(i = 0; i <= 6; i++)
	{
		tempDate.setDate(tempDate.getDate() + 1);
		dayOfWeek = tempDate.getDay();

		if(dayOfWeek == 5)
		{
			tempMonth = tempDate.getMonth();
			tempDay = tempDate.getDate();
			
			let paddedDay = '0' + tempDay;
			paddedDay = paddedDay.slice(-2);

			let paddedMonth = '0' + (tempMonth + 1);
			paddedMonth = paddedMonth.slice(-2);

			let dateKeyForFriday = paddedMonth + '-' + paddedDay;

			if(JS_DATA.ucWcsvIsActive == 1)
			{
				let fridayDateIndex = findIndexInArray(dateKeyForFriday, JS_WCSV_DATA);
				if(fridayDateIndex == -1)
				{
					dolog('jmonthDay Error in file (WCSV.js), date not found:' + dateKeyForFriday);
					return;
				}
				let fridayDataLine = JS_WCSV_DATA[fridayDateIndex];
				let fridayDataFields = fridayDataLine.split(',');
				fridayPrayerTime = fridayDataFields[13];
				secondJomoaDisplayHorizontal.innerHTML = '2nd Jomoa : ' + fridayDataFields[14];
			}
			else
			{
				let fridayDateIndex = findIndexInArray(dateKeyForFriday, JS_TIMES);
				if(fridayDateIndex == -1)
				{
					dolog('jmonthDay Error in file (wtimes), date not found:' + dateKeyForFriday);
					return;
				}
				let fridayDataLine = JS_TIMES[fridayDateIndex];
				let timesDataPartsFriday = fridayDataLine.split('~~~~~');
				let fridayDataFields = timesDataPartsFriday[1].split('|');
				fridayPrayerTime = fridayDataFields[prayerIndexForFriday];
			}

			break;
		}
	}

	if((isSummerTimeActive) && (tempMonth !== 10)) fridayPrayerTime = addOneHour(fridayPrayerTime);

	return fridayPrayerTime;
}




let isIqamaCounterActive = false;
let remainingSeconds = 0;
let prayerDurationMinutes = 0;
let iqamaMinutesParam = 0;
let totalCounterSeconds = 0;
let isDohaCounter = false;

function startIqamaCounterFunction(iqamaMinutes, prayerDuration, forcedCounterSeconds)
{


	if((!JS_DATA.ucIqamaCounter == 1) || (JS_DATA.ucIsForHome == 1)) return;

	if(iqamaMinutes < 1)
	{
	setTimeout('startIqamaSequenceFunction()', 1000);
	setTimeout('resetPrayerRowsStyleFunction()', 9000);
	return;
	}


			if(prayerDuration == 0)
			{
			isDohaCounter = true;
			iqamaCounterLabelVerticalElement.innerHTML = JS_eLang.cx_MinutesToDoha;
			iqamaCounterLabelHorizontalElement.innerHTML = JS_eLang.cx_MinutesToDoha;
			fullScreenLabelHorizontal.innerHTML = JS_eLang.cx_MinutesToDoha;
			fullScreenLabelVertical.innerHTML = JS_eLang.cx_MinutesToDoha;
			} 
		else 
			{
			isDohaCounter = false;
			iqamaCounterLabelVerticalElement.innerHTML = JS_eLang.cx_MinutesToIqama;
			iqamaCounterLabelHorizontalElement.innerHTML = JS_eLang.cx_MinutesToIqama;
			fullScreenLabelHorizontal.innerHTML = JS_eLang.cx_MinutesToIqama;
			fullScreenLabelVertical.innerHTML = JS_eLang.cx_MinutesToIqama;
			}


	let currentDateObjectForIqama = new Date();
	let currentSecondsComp = currentDateObjectForIqama.getSeconds();

	prayerDurationMinutes = prayerDuration;
	isIqamaCounterActive = true;
	remainingSeconds = (iqamaMinutes * 60) - currentSecondsComp;
	if((remainingSeconds <= 90)&&(JS_DATA.ucCounterColorAlert == 1)) isCounterColorAlert = true;
	iqamaMinutesParam = iqamaMinutes;
	
		 if(forcedCounterSeconds > 0) 
		 totalCounterSeconds = (forcedCounterSeconds * 60) - currentSecondsComp; 
	else totalCounterSeconds = remainingSeconds;
}






function adjustHijriDateFunction()
{
	JS_DATA.ucHijriDateFixer++;
	if(JS_DATA.ucHijriDateFixer > 3) JS_DATA.ucHijriDateFixer = -3;
	saveSettingsToStorageFunction();
	calculateAndDisplayTimesFunction();
}







function calculateNightPrayersFunction()
{

if(currentTimeInMinutes==0) return;



	let tomorrowDateObject = new Date;

	if( (JS_DATA.ucLocalHoursAdjustment !== 0) && (allowedHourAdjustments.indexOf(JS_DATA.ucLocalHoursAdjustment) !== -1) )
	{tomorrowDateObject.setTime(tomorrowDateObject.getTime() + (JS_DATA.ucLocalHoursAdjustment*60*60*1000));}
	


				if(currentTimeInMinutes > fajrTimeInMinutes)
				{
					tomorrowDateObject.setDate(tomorrowDateObject.getDate() + 1);
				}
				else
				{
					tomorrowDateObject.setDate(tomorrowDateObject.getDate());
				}	

	
	let tomorrowDayIndex = tomorrowDateObject.getDay();

	let currentYear = tomorrowDateObject.getFullYear();

	let currentMonthPadded = '0' + (tomorrowDateObject.getMonth() + 1);
	let currentDatePadded = '0' + tomorrowDateObject.getDate();

	currentDatePadded = currentDatePadded.slice(-2);
	currentMonthPadded = currentMonthPadded.slice(-2);

	let fajrTimeString = '';
	let maghribTimeString = '';



				if(JS_DATA.ucWcsvIsActive == 1)
				{
					let dateKey = currentMonthPadded + '-' + currentDatePadded;
					let dateIndex = findIndexInArray(dateKey, JS_WCSV_DATA);
					if(dateIndex == -1)
					{
						return;
					}
					let wcsvLine = JS_WCSV_DATA[idx];
					let wcsvFields = wcsvLine.split(',');
					fajrTimeString = wcsvFields[2];
					maghribTimeString = wcsvFields[2];
				}
	else
	{
		isSummerTimeAdjustment = true;
		let dateKeyForTimes = currentMonthPadded + '-' + currentDatePadded;
		let dateIndex = findIndexInArray(dateKeyForTimes, JS_TIMES);
		if(dateIndex == -1)
		{
			return;
		}

		let timesDataString = JS_TIMES[dateIndex];
		let timesDataParts = timesDataString.split('~~~~~');
		let prayerTimesArray = timesDataParts[1].split('|');
		fajrTimeString = prayerTimesArray[0];
		maghribTimeString = prayerTimesArray[4];
	}

	let tomorrowFajrTimeAdjusted = adjustAthanTime(fajrTimeString, JS_DATA.ucAthanMinutesFAJR);
	let tomorrowMaghribTimeAdjusted = adjustAthanTime(maghribTimeString, JS_DATA.ucAthanMinutesMGRB);

	if((isSummerTimeActive) && (isSummerTimeAdjustment))
	{
		let tomorrowDayIndex 	= tomorrowDateObject.getDay();
		let tomorrowMonth 		= tomorrowDateObject.getMonth();
		let tomorrowDate		= tomorrowDateObject.getDate();
		
		let isSpecialSummerTimeCase = ((tomorrowDayIndex == 0) && (tomorrowMonth == 9) && (tomorrowDate > 24));
		
		if(!isSpecialSummerTimeCase) 
		{
			tomorrowFajrTimeAdjusted = addOneHour(tomorrowFajrTimeAdjusted);
			tomorrowMaghribTimeAdjusted = addOneHour(tomorrowMaghribTimeAdjusted);
		}
	}


	
	
	let tomorrowFajrMinutes = timeStringToMinutesFunction(tomorrowFajrTimeAdjusted);
	

	let midnightMinutesCalc = ((1440 - maghribTimeInMinutes) + tomorrowFajrMinutes) / 2;
	    midnightMinutesCalc = midnightMinutesCalc+maghribTimeInMinutes;
	 if(midnightMinutesCalc >= 1440) midnightMinutesCalc = (midnightMinutesCalc - 1440);
	    midnightTimeInMinutes = Math.floor(midnightMinutesCalc);
	 
	let lastThirdMinutesCalc = ((1440 - maghribTimeInMinutes) + tomorrowFajrMinutes) / 3;
	    lastThirdMinutesCalc = (lastThirdMinutesCalc*2)+maghribTimeInMinutes;
	 if(lastThirdMinutesCalc >= 1440) lastThirdMinutesCalc = (lastThirdMinutesCalc - 1440);
	    lastThirdTimeInMinutes = Math.floor(lastThirdMinutesCalc);
	 	 
prayerTimesMinutesObject.NISF = midnightTimeInMinutes;
prayerTimesMinutesObject.TULT = lastThirdTimeInMinutes;



    let midnightTimeFormatted 	= minutesToTimeString(midnightTimeInMinutes);
    let lastThirdTimeFormatted 	= minutesToTimeString(lastThirdTimeInMinutes);

	let tomorrowFajrTimeFormatted 	= convertToArabicDigitsFunction(convertTo12HourFormat(tomorrowFajrTimeAdjusted));
	let midnightDisplayTime 	= convertToArabicDigitsFunction(convertTo12HourFormat(midnightTimeFormatted));
	let lastThirdDisplayTime 	= convertToArabicDigitsFunction(convertTo12HourFormat(lastThirdTimeFormatted));


	
	let nightPrayerSymbols = [" ", " ", " "];

	
	midnightTimeVerticalElement.innerHTML = midnightDisplayTime + "<span>"+nightPrayerSymbols[0]+"</span>";
	midnightTimeHorizontalElement.innerHTML = midnightDisplayTime + "<span>"+nightPrayerSymbols[0]+"</span>";
	
	lastThirdTimeVerticalElement.innerHTML = lastThirdDisplayTime + "<span>"+nightPrayerSymbols[1]+"</span>";
	lastThirdTimeHorizontalElement.innerHTML = lastThirdDisplayTime + "<span>"+nightPrayerSymbols[1]+"</span>";
	
	tomorrowFajrTimeVerticalElement.innerHTML = tomorrowFajrTimeFormatted + "<span>"+nightPrayerSymbols[2]+"</span>";
	tomorrowFajrTimeHorizontalElement.innerHTML = tomorrowFajrTimeFormatted + "<span>"+nightPrayerSymbols[2]+"</span>";



	if(encodedString3.length != 200+96) setElementAttribute2Function();
	if(encodedString4.length != 304+96) setElementAttributeFunction();



}






function enterFullScreenFunction()
{
	calculateAndDisplayTimesFunction();
	updateTimeAndPrayersFunction(false);
	launchFullscreen(document.documentElement);
	audioBeepElementVar.play();
}




function playAzanSoundFunction()
{
	if(JS_DATA.ucAzanIqamaByVoice == 1)
	{
		if(JS_DATA.ucShortAzanActive == 1)
		{
			if(isTawkitApp) redirectToAudioHandlerFunction(audioShortAzanElementVar.dataset.xfile);
			else audioShortAzanElementVar.play();
		}
		else
		{
			
				if(currentTimeInMinutes == fajrTimeInMinutes) 
				{
					if(isTawkitApp) redirectToAudioHandlerFunction(audioFajrAzanElement.dataset.xfile); else audioFajrAzanElement.play();
				}
				else 
				{
					if(isTawkitApp) redirectToAudioHandlerFunction(audioAzanMainElement.dataset.xfile); else audioAzanMainElement.play();
				}
		}
	}
	else
		{
		if(isTawkitApp) redirectToAudioHandlerFunction(audioBeepElementVar.dataset.xfile); else audioBeepElementVar.play();
		}

}




function playIqamaSoundFunction()
{
	if(JS_DATA.ucAzanIqamaByVoice == 1)
	{
				if(JS_DATA.ucShortIqamaActive == 1)
				{
					if(isTawkitApp) redirectToAudioHandlerFunction(audioShortIqamaElementVar.dataset.xfile); else audioShortIqamaElementVar.play();
				}
				else 
				{
					if(isTawkitApp) redirectToAudioHandlerFunction(audioTeetElementVar.dataset.xfile); else audioTeetElementVar.play();
				}
	}
	else
		{
		if(isTawkitApp) redirectToAudioHandlerFunction(audioTeetElementVar.dataset.xfile); else audioTeetElementVar.play();
		}
}




function playDohaAlertSoundFunction()
{
	if(isTawkitApp) redirectToAudioHandlerFunction(audioDrop3ElementVar.dataset.xfile); else audioDrop3ElementVar.play();
}


function testReminderSoundFunction()
{
	if(isTawkitApp) redirectToAudioHandlerFunction(audioAlertsElementVar.dataset.xfile); else audioAlertsElementVar.play();
}


function scrollMenuFunction(scrollableDivId, themeIndexParam)
{
	let scrollableElement = document.getElementById(scrollableDivId);

	let currentScrollTop = scrollableElement.scrollTop;
	if(themeIndexParam > 0) scrollableElement.scrollTop = currentScrollTop + 50;
	else scrollableElement.scrollTop = currentScrollTop - 50;
}




function timeStringToMinutesFunction(currentSeconds)
{
	
	let timeString = currentSeconds;
	let minutesPart = timeString.substr(0, 2);
	let hoursPart = timeString.substr(3, 2);
	return((parseInt(minutesPart) * 60) + parseInt(hoursPart));
}




function DisableSelection(elem)
{
	elem.onselectstart = function(){return false;};
	elem.unselectable = "on";
	elem.style.MozUserSelect = "none";
	elem.style.cursor = "default";
}




function launchFullscreen(element)
{
		 if(element.requestFullscreen)		 {element.requestFullscreen();}
	else if(element.mozRequestFullScreen)    {element.mozRequestFullScreen();}
	else if(element.webkitRequestFullscreen) {element.webkitRequestFullscreen();}
	else if(element.msRequestFullscreen)	 {element.msRequestFullscreen();}
}



function detectOrientationFunction()
{
isHorizontalOrientation = (window.innerHeight < window.innerWidth);
if((isHorizontalOrientation) && (JS_DATA.ucForcedVertical > 0)) isHorizontalOrientation = false;


		if(JS_DATA.ucMiniCounterOnLeftInVR == 1) 
		{
		rootElement.style.setProperty('--vrCounterTOP', '15.0%');
		rootElement.style.setProperty('--vrCounterLEFT', '5.9%');
		}
}



function transformCoordsVrRightFunction(xVirt, yVirt)
{
    const xView = viewportWidth - yVirt;
    const yView = xVirt;
    return { xView, yView };
}


function transformCoordsVrLeftFunction(xVirt, yVirt)
{
    const xView = yVirt;
    const yView = viewportHeight - xVirt;
    return { xView, yView };
}



let viewportWidth = 0;
let viewportHeight = 0;


function handleResizeFunction()
{

screenWidth	= window.screen.width;
screenHeight	= window.screen.height;

	if(JS_DATA.ucForcedVertical > 0)
	{
	screenWidth	= window.screen.height;
	screenHeight	= window.screen.width;	
	}
	
viewportWidth = window.innerWidth;
viewportHeight = window.innerHeight;

	detectOrientationFunction();
	applyThemeFunction(false);
	
	calculateNightPrayersFunction();
	updateMarqueeDisplayFunction();
}




function setElementDisabledStateFunction(disabledState, elementToDisable)
{
elementToDisable.disabled = disabledState;
}




function toggleAzanVoiceFunction()
{

		JS_DATA.ucAzanIqamaByVoice = +(!JS_DATA.ucAzanIqamaByVoice);
	
	azanVoiceCheckboxElement.checked = (JS_DATA.ucAzanIqamaByVoice == 1);
	saveSettingsToStorageFunction();
	updateVoiceCheckboxDependentsFunction();
	if(JS_DATA.ucAzanIqamaByVoice == 1) 
	{
	if(isTawkitApp) redirectToAudioHandlerFunction(audioBeepElementVar.dataset.xfile); else audioBeepElementVar.play();
	}
}




function updateVoiceCheckboxDependentsFunction()
{
	setElementDisabledStateFunction(!JS_DATA.ucAzanIqamaByVoice == 1, shortAzanCheckboxElement);
	setElementDisabledStateFunction(!JS_DATA.ucAzanIqamaByVoice == 1, shortIqamaCheckboxElement);
}




function toggleShortAzanFunction()
{
	JS_DATA.ucShortAzanActive = +(!JS_DATA.ucShortAzanActive);
	saveSettingsToStorageFunction();
	shortAzanCheckboxElement.checked = JS_DATA.ucShortAzanActive == 1;
}




function toggleShortIqamaFunction()
{
	JS_DATA.ucShortIqamaActive = +(!JS_DATA.ucShortIqamaActive);
	saveSettingsToStorageFunction();
	shortIqamaCheckboxElement.checked = (JS_DATA.ucShortIqamaActive == 1);
}




function toggleRamadanIshaFunction()
{
	JS_DATA.ucRamadanDoIsha30min = +(!JS_DATA.ucRamadanDoIsha30min);
	saveSettingsToStorageFunction();
	ramadanIshaCheckboxElement.checked = (JS_DATA.ucRamadanDoIsha30min == 1);
	calculateAndDisplayTimesFunction();
}




function toggleSummerTimeFunction()
{
	JS_DATA.ucInSummerAdd1Hour = +(!JS_DATA.ucInSummerAdd1Hour);
	saveSettingsToStorageFunction();
	summerTimeCheckboxElement.checked = (JS_DATA.ucInSummerAdd1Hour == 1);
	calculateAndDisplayTimesFunction();
}




function toggleForceOneHourMoreFunction()
{
	JS_DATA.ucForce1HourMore = +(!JS_DATA.ucForce1HourMore);
	saveSettingsToStorageFunction();
	forceOneHourMoreCheckboxElement.checked = (JS_DATA.ucForce1HourMore == 1);
	calculateAndDisplayTimesFunction();
}




function toggleForceOneHourLessFunction()
{
	JS_DATA.ucForce1HourLess = +(!JS_DATA.ucForce1HourLess);
	saveSettingsToStorageFunction();
	forceOneHourLessCheckboxElement.checked = (JS_DATA.ucForce1HourLess == 1);
	calculateAndDisplayTimesFunction();
}




function toggleMeteoActiveFunction()
{
	JS_DATA.ucIsMeteoOn = +(!JS_DATA.ucIsMeteoOn);
	saveSettingsToStorageFunction();
	updateMeteoVisibilityFunction(true);
}




function updateMeteoVisibilityFunction(updateMeteoDisplay)
{
	if(JS_DATA.ucIsMeteoOn == 1)
	{
		meteoActiveCheckboxElement.checked = true;
		rootElement.style.setProperty('--METEO', 'visible');
		if(updateMeteoDisplay) updateWeatherData();
	}
	else
	{
		meteoActiveCheckboxElement.checked = false;
		rootElement.style.setProperty('--METEO', 'hidden');
	}
}




function toggleMeteoWithPrayersFunction()
{
	JS_DATA.ucMeteoWithPrayers = +(!JS_DATA.ucMeteoWithPrayers);
	saveSettingsToStorageFunction();
	updateMeteoWithPrayersVisibilityFunction(true);
}




function updateMeteoWithPrayersVisibilityFunction(updateMeteoDisplay)
{
document.getElementById('meteoWithPrayersCheckbox').checked = (JS_DATA.ucMeteoWithPrayers == 1);

	if(JS_DATA.ucMeteoWithPrayers == 1)
		rootElement.style.setProperty('--MTOSLWTS', 'visible');
	else
		rootElement.style.setProperty('--MTOSLWTS', 'hidden');

	if(updateMeteoDisplay) updateWeatherData();
}




function toggle24HoursFunction()
{
	JS_DATA.ucActivate24Hours = +(!JS_DATA.ucActivate24Hours);
	saveSettingsToStorageFunction();
	use24HoursCheckboxElement.checked = (JS_DATA.ucActivate24Hours == 1);
	calculateAndDisplayTimesFunction();
	updateTimeAndPrayersFunction(false);
}







function toggleUseWcsvFunction()
{
	JS_DATA.ucWcsvIsActive = +(!JS_DATA.ucWcsvIsActive);
	saveSettingsToStorageFunction();
	updateWcsvModeFunction(JS_DATA.ucWcsvIsActive == 1,true);
}




function updateWcsvModeFunction(isWcsvActive,recalculateTimes)
{
	if(isWcsvActive)
	{
		useWcsvCheckboxElement.checked = true;
		rootElement.style.setProperty('--OPAK', '0');
	}
	else
	{
		useWcsvCheckboxElement.checked = false;
		rootElement.style.setProperty('--OPAK', '1');
	}

	if(recalculateTimes) updateTimeAndPrayersFunction(false);
}



function togglePsFlagFunction()
{
	JS_DATA.ucPsFlag = +(!JS_DATA.ucPsFlag);
	saveSettingsToStorageFunction();
	updatePsFlagVisibilityFunction();
}




function updatePsFlagVisibilityFunction()
{
	let psFlagVisibility = 'hidden';
	if(JS_DATA.ucPsFlag == 1) psFlagVisibility = 'visible'; 
	rootElement.style.setProperty('--PSFLG', psFlagVisibility);
	document.getElementById('psFlagCheckbox').checked = (JS_DATA.ucPsFlag == 1);
}

function toggleDimPastPrayersFunction()
{
	JS_DATA.ucDimmPastPrayers = +(!JS_DATA.ucDimmPastPrayers);
	saveSettingsToStorageFunction();
	updateDimPastPrayersCheckboxFunction();
	applyDimPastPrayers(currentTimeInMinutes);
}

function updateDimPastPrayersCheckboxFunction()
{
	document.getElementById('dimPastPrayersCheckbox').checked = (JS_DATA.ucDimmPastPrayers == 1);
}






function toggleNightPrayersFunction()
{
	JS_DATA.ucShowNightPrayers = +(!JS_DATA.ucShowNightPrayers);
	saveSettingsToStorageFunction();
	updateNightPrayersVisibilityFunction();
}

function updateNightPrayersVisibilityFunction()
{
	document.getElementById('showNightPrayersCheckbox').checked = (JS_DATA.ucShowNightPrayers == 1);
	
	if(JS_DATA.ucShowNightPrayers == 1)
	{
		showElementFunction('nightPrayersContainerVertical');
		showElementFunction('nightPrayersContainerHorizontal');
	}
	else
	{
		hideElementByIdFunction('nightPrayersContainerVertical');
		hideElementByIdFunction('nightPrayersContainerHorizontal');
	}
}





function toggleIqamaFullTimesFunction()
{
	JS_DATA.ucIqamaFullTimes = +(!JS_DATA.ucIqamaFullTimes);
	saveSettingsToStorageFunction();
	fullIqamaTimesCheckboxElement.checked = (JS_DATA.ucIqamaFullTimes == 1);
	updateIqamaTimesDisplayFunction();
}




function toggleIqamaCounterFunction()
{
	JS_DATA.ucIqamaCounter = +(!JS_DATA.ucIqamaCounter);
	saveSettingsToStorageFunction();
	iqamaCounterCheckboxElement.checked = (JS_DATA.ucIqamaCounter == 1);
}





function toggleClockFullFunction()
{
	JS_DATA.ucClockIsFull = +(!JS_DATA.ucClockIsFull);
	saveSettingsToStorageFunction();
	updateClockDisplayFunction(JS_DATA.ucClockIsFull == 1);
}


function updateClockDisplayFunction(isFullClock)
{
fullClockCheckboxElement.checked = (isFullClock);

	if(isFullClock)
	{
		hideElementByIdFunction('fullClockContainerVertical');
		hideElementByIdFunction('fullClockContainerHorizontal');
		showElementFunction('miniClockContainerVertical');
		showElementFunction('miniClockContainerHorizontal');
	}
	else
	{
		showElementFunction('fullClockContainerVertical');
		showElementFunction('fullClockContainerHorizontal');
		hideElementByIdFunction('miniClockContainerVertical');
		hideElementByIdFunction('miniClockContainerHorizontal');
	}

}




function toggleBlackScreenInPrayingFunction()
{
	JS_DATA.ucBlackScreenInPraying = +(!JS_DATA.ucBlackScreenInPraying);
	saveSettingsToStorageFunction();
	blackScreenInPrayingCheckboxElement.checked = (JS_DATA.ucBlackScreenInPraying == 1);
}


function toggleRemindersActiveFunction()
{
	JS_DATA.ucRemindersActive = +(!JS_DATA.ucRemindersActive);
	saveSettingsToStorageFunction();
	remindersActiveCheckboxElement.checked = (JS_DATA.ucRemindersActive == 1);
}


function toggleSlidesActiveFunction()
{
	JS_DATA.ucSlidesActive = +(!JS_DATA.ucSlidesActive);
	saveSettingsToStorageFunction();
	slidesActiveCheckboxElement.checked = (JS_DATA.ucSlidesActive == 1);
}



function toggleSlidesRandomFunction()
{
	JS_DATA.ucSlidesRandom = +(!JS_DATA.ucSlidesRandom);
	saveSettingsToStorageFunction();
	slidesRandomCheckboxElement.checked = (JS_DATA.ucSlidesRandom == 1);
}


function toggleAzkarActiveFunction()
{
	JS_DATA.ucAzkarActive = +(!JS_DATA.ucAzkarActive);
	saveSettingsToStorageFunction();
	azkarActiveCheckboxElement.checked = (JS_DATA.ucAzkarActive == 1);
	setElementDisabledStateFunction((JS_DATA.ucAzkarActive == 0), azkar5minPictureCheckboxElement);
}


function toggleRepeatAzkarFunction()
{
	JS_DATA.ucRepeatMainAzkarOnce = +(!JS_DATA.ucRepeatMainAzkarOnce);
	saveSettingsToStorageFunction();
	repeatAzkarCheckboxElement.checked = (JS_DATA.ucRepeatMainAzkarOnce > 0);
	
	duplicateMainAzkar();
}


function duplicateMainAzkar()
{


if(JS_DATA.ucRepeatMainAzkarOnce < 1 ) return; 
if(JS_DATA.ucRepeatMainAzkarOnce > 2 ) return; 



mainAzkarArray = [];
let tempAzkarArray = [];

 
 
	let azkarBlock = "";
	let sourceAzkarArray = [];
	if(isHorizontalOrientation) sourceAzkarArray = JS_GLOBAL_AZKAR_HR; else sourceAzkarArray = JS_GLOBAL_AZKAR_VR;


			for(let i = 0; i < sourceAzkarArray.length; i++)
			{
				if(sourceAzkarArray[i] !== "")
				{
				let azkarLine = sourceAzkarArray[i];
					if(azkarLine.indexOf('-----') > -1)
					{
					if(azkarBlock !== "") tempAzkarArray.push(azkarBlock);
					azkarBlock = "";
					}
					else {azkarBlock += azkarLine + "<br>";}
				}
			}


mainAzkarArray = tempAzkarArray;

mainAzkarArray = mainAzkarArray.concat(tempAzkarArray);

if (JS_DATA.ucRepeatMainAzkarOnce == 2 )
mainAzkarArray = mainAzkarArray.concat(tempAzkarArray);

}



function toggleAzkar5minPictureFunction()
{
	JS_DATA.ucAzkar5minPicture = +(!JS_DATA.ucAzkar5minPicture);
	saveSettingsToStorageFunction();
	updateAzkar5minCheckboxes();
}


function toggleAzkarBgWhiteFunction()
{
	JS_DATA.ucAzkarBGwhite = +(!JS_DATA.ucAzkarBGwhite);
	saveSettingsToStorageFunction();
	updateAzkar5minCheckboxes();
}


function updateAzkar5minCheckboxes()
{
	azkar5minPictureCheckboxElement.checked = (JS_DATA.ucAzkar5minPicture == 1);
	document.getElementById('azkarBgWhiteCheckbox').checked = (JS_DATA.ucAzkarBGwhite == 1);
}


function toggleAyatsLangListFunction()
{
const ayatsLanguageSelectorElement = document.getElementById('ayatsLanguageSelector');
const ayatsLangListContainer = document.getElementById('ayatsLangListContainer');

ayatsLangListContainer.style.display = ayatsLangListContainer.style.display === 'block' ? 'none' : 'block';
 
}


let lastAyaIndex = 0;
let randomAyaIndex = 0;
let recursionCounter = 0;


function displayRandomAyaFunction()
{

let ayaSpecialOffset = 0;
let selectedLanguageCode = "";
recursionCounter++;




	if(recursionCounter > 20)
	{
	selectedLanguageCode = "error _RECURSIONS : ayats-"+JS_DATA.ucAyatsLANG;
	recursionCounter = 0;
	ayaDisplayVerticalElement.innerHTML = selectedLanguageCode;
	ayaDisplayHorizontalElement.innerHTML = selectedLanguageCode;
	return;
	}
	
	

	randomAyaIndex = Math.floor((Math.random() * JS_AYATS.length));


	if(lastAyaIndex == randomAyaIndex)
	{
		displayRandomAyaFunction();
		return;
	}

	selectedLanguageCode = JS_AYATS[randomAyaIndex];

	
	if(selectedLanguageCode == "")
	{
		displayRandomAyaFunction();
		return;
	}


	if(!isHorizontalOrientation)
	{
		
		let ayaTextLength = countArabicCharactersFunction(selectedLanguageCode);
			if(selectedLanguageCode.indexOf('<span>') > -1) 
			{
				ayaTextLength = ayaTextLength-19;
				ayaSpecialOffset = 70;
			}
		
		if(ayaTextLength > (JS_VR_AYA_MAXSIZE + ayaSpecialOffset) ) 
		{
		displayRandomAyaFunction();
		return;
		}
	}




	if((isFirstTenDhulHijjah) &&
		((selectedLanguageCode.indexOf('10NIGHTS') > -1) || (selectedLanguageCode.indexOf('3WHITEDAYS') > -1) || (selectedLanguageCode.indexOf('TAKBIR') > -1)))
	{
		displayRandomAyaFunction();
		return;
	}

	if((isLastTenRamadan) &&
		((selectedLanguageCode.indexOf('10DAYS') > -1) || (selectedLanguageCode.indexOf('3WHITEDAYS') > -1) || (selectedLanguageCode.indexOf('TAKBIR') > -1)))
	{
		displayRandomAyaFunction();
		return;
	}

	if((isWhiteDaysPeriod) &&
		((selectedLanguageCode.indexOf('10DAYS') > -1) || (selectedLanguageCode.indexOf('10NIGHTS') > -1) || (selectedLanguageCode.indexOf('TAKBIR') > -1)))
	{
		displayRandomAyaFunction();
		return;
	}

	if((isEidOrTashreeq) &&
		((selectedLanguageCode.indexOf('10DAYS') > -1) || (selectedLanguageCode.indexOf('10NIGHTS') > -1) || (selectedLanguageCode.indexOf('3WHITEDAYS') > -1)))
	{
		displayRandomAyaFunction();
		return;
	}

	let isSpecialPeriod = (isFirstTenDhulHijjah || isLastTenRamadan || isWhiteDaysPeriod || isEidOrTashreeq);
	let hasSpecialKeyword = (selectedLanguageCode.indexOf('10DAYS') > -1) || (selectedLanguageCode.indexOf('10NIGHTS') > -1) || (selectedLanguageCode.indexOf('3WHITEDAYS') > -1) || (selectedLanguageCode.indexOf('TAKBIR') > -1);

	if(hasSpecialKeyword && !isSpecialPeriod)
	{
		displayRandomAyaFunction();
		return;
	}


	selectedLanguageCode = selectedLanguageCode.replace('TAKBIR', '');
	selectedLanguageCode = selectedLanguageCode.replace('10DAYS', '');
	selectedLanguageCode = selectedLanguageCode.replace('10NIGHTS', '');
	selectedLanguageCode = selectedLanguageCode.replace('3WHITEDAYS', '');

	if(hasSpecialKeyword) selectedLanguageCode = "<div class='specialAyaClass'>" + selectedLanguageCode + "</div>";

	selectedLanguageCode = selectedLanguageCode.replace(/\{/g, "﴿");
	selectedLanguageCode = selectedLanguageCode.replace(/\}/g, "﴾");
	selectedLanguageCode = selectedLanguageCode.replace(/\|/g, "<br>");

	if(isTestModeActive) selectedLanguageCode = "";
	
	let ayaCharacterCount = countArabicCharactersFunction(selectedLanguageCode);
	


let calculatedAyaFontSize = 3.6;
	
	if(isHorizontalOrientation) 
	{
		if(selectedLanguageCode.indexOf('<span>') == -1)
		calculatedAyaFontSize = calculateFontSizeFunction(ayaCharacterCount, JS_HR_AYA_MAXSIZE, 21, 4.7, 3.5);
		else
		calculatedAyaFontSize = calculateFontSizeFunction(ayaCharacterCount, JS_HR_AYA_MAXSIZE, 21, 4.6, 3.9);
	}
else
	{
		if(selectedLanguageCode.indexOf('<span>') == -1)  
		calculatedAyaFontSize = calculateFontSizeFunction(ayaCharacterCount, JS_VR_AYA_MAXSIZE, 21, 7.8, 6.1);
		else
		calculatedAyaFontSize = calculateFontSizeFunction(ayaCharacterCount, JS_VR_AYA_MAXSIZE, 21, 7.7, 7.0);
	}


	
	let ayaFontSizeVertical = calculatedAyaFontSize;
	let ayaFontSizeHorizontal = calculatedAyaFontSize;

	ayaContainerVertical.style.fontSize = ayaFontSizeVertical+fontSizeUnit;
	ayaContainerHorizontal.style.fontSize = ayaFontSizeHorizontal+fontSizeUnit;


	ayaDisplayVerticalElement.innerHTML = selectedLanguageCode;
	ayaDisplayHorizontalElement.innerHTML = selectedLanguageCode;

	lastAyaIndex = randomAyaIndex;
	recursionCounter = 0;
}






const CHARCODE_SHADDA 			= 1617;
const CHARCODE_SUKOON 			= 1618;
const CHARCODE_SUPERSCRIPT_ALIF = 1648;
const CHARCODE_TATWEEL 			= 1600;
const CHARCODE_ALIF 			= 1575;
 

function countArabicCharactersFunction(sourceString)
{

let ArabsourceString = sourceString;
let currentChar = "";
let charCode = 0;
let charCount = 0;

  for (let i = 0; i < ArabsourceString.length; i++)
  {
    currentChar = ArabsourceString.charAt(i);
    if((typeof(currentChar) == 'undefined' || currentChar == null)) continue;
    charCode = currentChar.charCodeAt(0);
    if((charCode == CHARCODE_TATWEEL || charCode == CHARCODE_SUPERSCRIPT_ALIF || charCode >= 1612 && charCode <= 1631)) continue;
    charCount++;                     
  }

return charCount;                   
}




let JsLASTj = 0;
let JsNBj = 0;

function displayRandomHadithFunction()
{

	JsNBj = Math.floor((Math.random() * JS_AHADITH.length));

	if(JsLASTj == JsNBj)
	{
		displayRandomHadithFunction();
		return;
	}

	let selectedHadith = JS_AHADITH[JsNBj];
	
	if(selectedHadith == "")
	{
		displayRandomHadithFunction();
		return;
	}



	if(!isHorizontalOrientation)
	{
	let hadithLength = countArabicCharactersFunction(selectedHadith);
		if(hadithLength > JS_VR_HADITH_SIZE) 
		{
		displayRandomHadithFunction();
		return;
		}
	}

	
	hadithDisplayVerticalElement.innerHTML = selectedHadith;
	hadithDisplayHorizontalElement.innerHTML = selectedHadith;


JsLASTj = JsNBj;
}

let nightPrayersClickCounter = 0;

function toggleNightPrayersDisplayFunction()
{
	nightPrayersClickCounter++;
	if(nightPrayersClickCounter > 2)
	{
	nightPrayersClickCounter = 0;
	isTestModeActive = !isTestModeActive;
	displayRandomAyaFunction();
	}
}




let inputDialogValue		= "";
let pendingCallback = "";
let isInputDialogOpen 		= false;

 
function openInputDialogFunction(dialogTitle, defaultValue, callbackFunction, isLtrInput)
{
pendingCallback = callbackFunction;
inputDialogMessageElement.innerHTML = dialogTitle;

  inputDialogInputElement.value = "";
  inputDialogInputElement.value = defaultValue;
  
  
		if(isLtrInput) 
		{
		inputDialogInputElement.style.direction = 'ltr';
		inputDialogInputElement.style.textAlign = 'left';
		}
		else 
			{
			inputDialogInputElement.style.direction = "var(--csvar_nowDIR)";
			if(isRTL) inputDialogInputElement.style.textAlign = 'right'; 
			}
showElementFunction("inputDialogContainer");


  setTimeout(() => {
    inputDialogInputElement.blur();
    inputDialogInputElement.focus();
  }, 50);
  
}


function confirmInputDialogFunction()
{
isInputDialogOpen   = true;
inputDialogValue = inputDialogInputElement.value;

inputDialogInputElement.blur(); 
hideElementByIdFunction("inputDialogContainer");

eval(pendingCallback);
isInputDialogOpen	= false;


}


function cancelInputDialogFunction()
{
inputDialogInputElement.blur(); 
hideElementByIdFunction("inputDialogContainer");
}
 

function editMosqueNameFunction()
{

	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_MenuMosqueNAME, JS_DATA.ucMosqueName, 'editMosqueNameFunction()', false);
	return;
	}
	
	JS_DATA.ucMosqueName = inputDialogValue;
	mosqueNameVerticalElement.innerHTML = JS_DATA.ucMosqueName;
	mosqueNameHorizontalElement.innerHTML = JS_DATA.ucMosqueName;
	saveSettingsToStorageFunction();

}


function openCountryListFunction()
{
showElementFunction("countryListModalId");

}




function setThemeTypeFunction(themeIndexParam)
{
JS_DATA.ucThemesActiveType = themeIndexParam;
saveSettingsToStorageFunction();
setThemeTypeRadioButton();

currentThemeIndexForPrayer = -1;
applyThemeChange();
}


function setThemeTypeRadioButton()
{
document.getElementById('BGsType_' + JS_DATA.ucThemesActiveType).checked = true;
}








function editThemeForSalatFunction(themeIndexParam)
{

	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Enter Theme Number for : "+prayerNamesArray[themeIndexParam], themesForEachSalat[themeIndexParam], 'editThemeForSalatFunction('+themeIndexParam+')', true);
	return;
	}

	let parsedThemeNumber = parseInt(inputDialogValue);
	if( (inputDialogValue !== '.') && (!(parsedThemeNumber >= 0 && parsedThemeNumber <= 39)) )return; 
	
	themesForEachSalat[themeIndexParam] = inputDialogValue;
	saveThemesForEachSalat();
	saveSettingsToStorageFunction();
	
	currentThemeIndexForPrayer = -1;
	applyThemeChange();
}


function saveThemesForEachSalat()
{
JS_DATA.ucThemes4eachSalat = '';
	for(let i=0; i <=5; i++)
	{
	JS_DATA.ucThemes4eachSalat += themesForEachSalat[i]+'|';
	document.getElementById('jm'+i).innerHTML = themesForEachSalat[i];
	}
}









function editThemeForDayFunction(themeIndexParam)
{

	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Enter Theme number for : "+weekDayNamesArray[themeIndexParam], dailyThemesArray[themeIndexParam], 'editThemeForDayFunction('+themeIndexParam+')', true);
	return;
	}
	
	
	let parsedThemeNumber = parseInt(inputDialogValue);
	if( (inputDialogValue !== '.') && (!(parsedThemeNumber >= 0 && parsedThemeNumber <= 39)) )return; 
	dailyThemesArray[themeIndexParam] = inputDialogValue;
	saveThemesForEachDay();
	saveSettingsToStorageFunction();
	applyThemeChange();
}


function saveThemesForEachDay()
{
JS_DATA.ucThemes4EveryDays = '';
	for(let i=0; i <=6; i++)
	{
	JS_DATA.ucThemes4EveryDays += dailyThemesArray[i]+'|';
	document.getElementById( 'jj' + i).innerHTML = dailyThemesArray[i];
	}
}





function editThemeRandomListFunction()
{

	
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Enter the numbers of Themes you like <br>(numbers separated by comma)", randomThemesList.toString(), 'editThemeRandomListFunction()', true);
	return;
	}
	
	let selectedLanguageCode = inputDialogValue;
	randomThemesList = selectedLanguageCode.split(',');
	JS_DATA.ucThemesMyBGsLista = randomThemesList.toString();
	displayRandomThemesList();
	saveSettingsToStorageFunction();
}

function displayRandomThemesList()
{
document.getElementById('themeRandomListSpanElement').innerHTML = JS_DATA.ucThemesMyBGsLista;
}




function editMobileAlertFunction()
{

	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Alert Before Iqama :", JS_DATA.ucCloseMobilePlease, 'editMobileAlertFunction()', false);
	return;
	}
	
	
	JS_DATA.ucCloseMobilePlease = inputDialogValue;
	saveSettingsToStorageFunction();
	mobileAlertButtonElement.innerHTML = JS_DATA.ucCloseMobilePlease;
}


const clockAdjustSpan = document.getElementById('clockAdjustValueSpanElement');

function editClockAdjustFunction()
{

	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_ClockADJUST, JS_DATA.ucLocalHoursAdjustment, 'editClockAdjustFunction()', true);
	return;
	}
	
	let adjustedHoursValue = parseInt(inputDialogValue); 
	if(allowedHourAdjustments.indexOf(adjustedHoursValue) == -1) adjustedHoursValue = 0;
	
	JS_DATA.ucLocalHoursAdjustment = adjustedHoursValue;
	saveSettingsToStorageFunction();
	updateClockAdjustDisplay();
	updateTimeAndPrayersFunction(false);
	calculateAndDisplayTimesFunction();
}


function updateClockAdjustDisplay()
{
clockAdjustSpan.innerHTML = JS_DATA.ucLocalHoursAdjustment;
}


function editEidFitrTimeFunction()
{

	
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_Salat_Eid_FITR, JS_DATA.ucTimeOfEidFITR, 'editEidFitrTimeFunction()', true);
	return;
	}
	
	
	JS_DATA.ucTimeOfEidFITR = inputDialogValue;
	saveSettingsToStorageFunction();
	document.getElementById('eidFitrTimeButton').innerHTML = JS_DATA.ucTimeOfEidFITR;
	updateEidMessageVisibility();
}

function editEidAdhaTimeFunction()
{

	
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_Salat_Eid_ADHA, JS_DATA.ucTimeOfEidADHA, 'editEidAdhaTimeFunction()', true);
	return;
	}
	
	
	JS_DATA.ucTimeOfEidADHA = inputDialogValue;
	saveSettingsToStorageFunction();
	document.getElementById('eidAdhaTimeButton').innerHTML = JS_DATA.ucTimeOfEidADHA;
	updateEidMessageVisibility();
}


let eidMessageContainerHorizontal = document.getElementById('eidMessageContainerHorizontal');
let eidMessageContainerVertical = document.getElementById('eidMessageContainerVertical');

function updateEidMessageVisibility()
{

let isEidActive = false;


	if((isEidFitrPeriod)&&(JS_DATA.ucActivateEidFITR==1))
	{
	eidMessageContainerHorizontal.innerHTML = JS_eLang.cx_Salat_Eid_FITR+" "+JS_DATA.ucTimeOfEidFITR;
	eidMessageContainerVertical.innerHTML = eidMessageContainerHorizontal.innerHTML;
	showElementFunction('eidMessageContainerHorizontal');
	showElementFunction('eidMessageContainerVertical');
	isEidActive = true;
	}

	
	if((isEidAdhaPeriod)&&(JS_DATA.ucActivateEidADHA==1))
	{
	eidMessageContainerHorizontal.innerHTML = JS_eLang.cx_Salat_Eid_ADHA+" "+JS_DATA.ucTimeOfEidADHA;
	eidMessageContainerVertical.innerHTML = eidMessageContainerHorizontal.innerHTML;
	showElementFunction('eidMessageContainerHorizontal');
	showElementFunction('eidMessageContainerVertical');
	isEidActive = true;
	}	

		if(isEidActive)
		{
		if(verticalMainContainer.className == 'specialVrClass') showElementFunction('mosqueNameDisplayVertical'); else hideElementByIdFunction('mosqueNameDisplayVertical');
		if(JS_DATA.ucDateUpRightInHR == 1) showElementFunction('mosqueNameDisplayHorizontal'); else hideElementByIdFunction('mosqueNameDisplayHorizontal');
		return;
		}



hideElementByIdFunction('eidMessageContainerHorizontal');
hideElementByIdFunction('eidMessageContainerVertical');

showElementFunction('mosqueNameDisplayVertical');
showElementFunction('mosqueNameDisplayHorizontal');

}




function editDohrXminAsrFunction()
{
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Enter Dohr-Azan, X-minutes before Asr<br>(enter 0 to disable)", JS_DATA.ucDohrXminutesAsr, 'editDohrXminAsrFunction()', true);
	return;
	}

	JS_DATA.ucDohrXminutesAsr = parseInt(inputDialogValue);
	if(!JS_DATA.ucDohrXminutesAsr) JS_DATA.ucDohrXminutesAsr = 0;
	saveSettingsToStorageFunction();
	updateDohrXminAsrDisplay();
	updateMosqueAndDateDisplayFunction();
	calculateAndDisplayTimesFunction();
}

function updateDohrXminAsrDisplay()
{
document.getElementById('dohrXminAsrValue').innerHTML = JS_DATA.ucDohrXminutesAsr;
}




function editPrimaryAzanFunction()
{
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("(وقت الأذان الأول) Enter minutes before Fajr<br>(enter 0 to disable)", JS_DATA.ucPrimaryAzanMinutes, 'editPrimaryAzanFunction()', true);
	return;
	}

	JS_DATA.ucPrimaryAzanMinutes = parseInt(inputDialogValue);
	if(!JS_DATA.ucPrimaryAzanMinutes) JS_DATA.ucPrimaryAzanMinutes = 0;
	saveSettingsToStorageFunction();
	updatePrimaryAzanDisplay();
}

function updatePrimaryAzanDisplay()
{
document.getElementById('primaryAzanValue').innerHTML = JS_DATA.ucPrimaryAzanMinutes;
}



function editSlidesViewTimeFunction()
{
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_SlidesViewTime + "<br>" + JS_eLang.cx_SlidesScreenMaxMin, JS_DATA.ucSlidesViewTime, 'editSlidesViewTimeFunction()', true);
	return;
	}

	JS_DATA.ucSlidesViewTime = parseInt(inputDialogValue);
	if(!JS_DATA.ucSlidesViewTime) JS_DATA.ucSlidesViewTime = 15;
	clampSlidesViewTimeFunction();
	saveSettingsToStorageFunction();
	updateSlidesViewTimeDisplay();
}

function updateSlidesViewTimeDisplay()
{
document.getElementById('slidesViewTimeValue').innerHTML = JS_DATA.ucSlidesViewTime;
}




function editTawkitViewTimeFunction()
{
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_TawkitViewTime + "<br>" + JS_eLang.cx_SlidesScreenMaxMin, JS_DATA.ucTawkitViewTime, 'editTawkitViewTimeFunction()', true);
	return;
	}

	JS_DATA.ucTawkitViewTime = parseInt(inputDialogValue);
	if(!JS_DATA.ucTawkitViewTime) JS_DATA.ucTawkitViewTime = 15;
	clampSlidesViewTimeFunction();
	saveSettingsToStorageFunction();
	updateTawkitViewTimeDisplay();
}

function updateTawkitViewTimeDisplay()
{
document.getElementById('tawkitViewTimeValue').innerHTML = JS_DATA.ucTawkitViewTime;
}




let iqamaTestCounter = 0;

function triggerIqamaTestFunction()
{
if(!isTestModeActive) return;

	iqamaTestCounter++;
	if(iqamaTestCounter >= 3)
	{
		iqamaTestCounter = 0;
		startIqamaCounterFunction(2, 1, 0);
		showIqamaCounter();
	}
}


let azkarMainTestCounter = 0;

function triggerAzkarMainFunction()
{
if(!isTestModeActive) return;

	azkarMainTestCounter++;
	if(azkarMainTestCounter >= 3)
	{
		azkarMainTestCounter = 0;
		startAzkarMainSequenceFunction();
	}
}

let slidesTestCounter = 0;

function triggerSlidesFunction()
{
if(!isTestModeActive) return;

if(slidesDisplayActive) return; 

	slidesTestCounter++;
	if(slidesTestCounter >= 3)
	{
		slidesTestCounter = 0;
		startSlidesSequenceFunction();
	}
}

let azkarSabahTestCounter = 0;




function triggerAzkarSabahFunction()
{
if(!isTestModeActive) return;

	azkarSabahTestCounter++;
	if(azkarSabahTestCounter >= 3)
	{
		azkarSabahTestCounter = 0;
		startAzkarSequenceFunction(sabahAzkarArray, false, true);
	}
}

let azkarMasaaTestCounter = 0;




function triggerAzkarMasaaFunction()
{
if(!isTestModeActive) return;

	azkarMasaaTestCounter++;
	if(azkarMasaaTestCounter >= 3)
	{
		azkarMasaaTestCounter = 0;
		startAzkarSequenceFunction(masaaAzkarArray, false, false);
	}
}



let isMenuOpen = true;

const menuToggleButtonVertical = document.getElementById('menuToggleButton');
const menuToggleButtonHorizontal = document.getElementById('menuToggleButtonHorizontal');




function toggleMenuFunction()
{
	isMenuOpen = !isMenuOpen;
	if(isMenuOpen) hideElementByIdFunction('mainMenuContainer'); else showElementFunction('mainMenuContainer');
}




function closeMenuFunction()
{
	isMenuOpen = !isMenuOpen;
	hideElementByIdFunction('mainMenuContainer');
}



function addMessageFunction()
{

	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Enter your message. اكتب إعلانك ", "", 'addMessageFunction()', false);
	return;
	}

	let selectedLanguageCode = inputDialogValue.replace(/"/g, '“');
	JS_BOTTOM_MSGS.unshift("1|DAILY|" + selectedLanguageCode);
	refreshBottomMessages();

}




function refreshBottomMessages()
{
	saveBottomMessages();
	displayBottomMessagesTable();
	updateMarqueeText();
	updateMarqueeDisplayFunction();
}




function updateBottomMessages()
{
	updateMarqueeText();
	updateMarqueeDisplayFunction();
}


function sanitizeInputString(inputString)
{
    if (typeof inputString !== 'string') return '';
    
    let sanitizedString = inputString.replace(/\"/g, '“'); 
    sanitizedString = sanitizedString.replace(/\\[°#^@\.]/g, '');
    
    return sanitizedString.trim();
}


function addSlideFunction()
{

	
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Enter your slide message here<br>or add picture file name (.jpg)<br>or link of your image http://....", "", 'addSlideFunction()', false);
	return;
	}

	let selectedLanguageCode = inputDialogValue;
	selectedLanguageCode = sanitizeInputString(selectedLanguageCode);
	JS_SLIDES_DATA.push(selectedLanguageCode);
	refreshSlides();


}



function addReminderFunction()
{
	
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Enter Reminder<br> example of alert 5 minutes before maghrib :<br>  5.BEFORE.MGRB.()", "", 'addReminderFunction()', true);
	return;
	}

	let selectedLanguageCode = sanitizeInputString(inputDialogValue);
	JS_REMINDERS_DATA.push(selectedLanguageCode);
	refreshReminders();


}


function refreshReminders()
{
	saveRemindersToStorage();
	displayRemindersTable();
}


function saveRemindersToStorage()
{
	JS_REMINDERS = ""; 
	for(let i = 0; i < JS_REMINDERS_DATA.length; i++)
	{
		let tempMessageItem = JS_REMINDERS_DATA[i];
		if(tempMessageItem !== "") JS_REMINDERS += tempMessageItem + "°°";
	}
	localStorage.setItem('STORAGE_JS_REMINDERS', JS_REMINDERS);
}




function displayRemindersTable()
{
	clearRemindersTable();

	for(let i = 0; i < JS_REMINDERS_DATA.length; i++)
	{
		let reminderItem = JS_REMINDERS_DATA[i];
		if(reminderItem !== "")
		{
			let tableRow = remindersTableBody.insertRow(-1);
			let actionCell = tableRow.insertCell(0);
			let contentCell = tableRow.insertCell(1);
			let deleteCell = tableRow.insertCell(2);

			actionCell.innerHTML = "<span class='cursorPointerClass' onclick='editReminder(" + i + ")'>EDIT</span>";
			contentCell.innerHTML = "<span class='reminderContentClass'>" + reminderItem + "</span>";
			deleteCell.innerHTML = "<span class='cursorPointerClass' onclick='deleteReminder(" + i + ")'> X </span>";
		}
	}
}




function deleteReminder(arrayIndex)
{
	JS_REMINDERS_DATA.splice(arrayIndex, 1);
	refreshReminders();
}




function editReminder(arrayIndex)
{
	let tempReminderItem = JS_REMINDERS_DATA[arrayIndex];


	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Edit Reminder", tempReminderItem, 'editReminder('+arrayIndex+')', true);
	return;
	}

	let selectedLanguageCode = inputDialogValue.replace(/"/g, '“');
		JS_REMINDERS_DATA[arrayIndex] = selectedLanguageCode;
		refreshReminders();
}


function clearRemindersTable()
{
	while(remindersTableBody.hasChildNodes())
	{
		remindersTableBody.removeChild(remindersTableBody.firstChild);
	}
}







function clearSlidesTable()
{
	while(slidesTableBody.hasChildNodes())
	{
		slidesTableBody.removeChild(slidesTableBody.firstChild);
	}
}




function refreshSlides()
{
	saveSlidesToStorage();
	displaySlidesTable();
}




function saveSlidesToStorage()
{
	JS_SLIDES = "";
	for(let i = 0; i < JS_SLIDES_DATA.length; i++)
	{
		let tempMessageItem = JS_SLIDES_DATA[i];
		if(tempMessageItem !== "") JS_SLIDES += tempMessageItem + "°°";
	}
	localStorage.setItem('STORAGE_JS_SLIDES', JS_SLIDES);
}




function displaySlidesTable()
{
	clearSlidesTable();

	for(let i = 0; i < JS_SLIDES_DATA.length; i++)
	{
		let reminderItem = JS_SLIDES_DATA[i];
		if(reminderItem !== "")
		{
			let tableRow = slidesTableBody.insertRow(-1);
			let actionCell = tableRow.insertCell(0);
			let contentCell = tableRow.insertCell(1);
			let deleteCell = tableRow.insertCell(2);

			actionCell.innerHTML = "<span class='cursorPointerClass' onclick='editSlide(" + i + ")'>EDIT</span>";
			contentCell.innerHTML = "<span class='reminderContentClass'>" + reminderItem + "</span>";
			deleteCell.innerHTML = "<span class='cursorPointerClass' onclick='deleteSlide(" + i + ")'> X </span>";
		}
	}
}




function deleteSlide(arrayIndex)
{
	JS_SLIDES_DATA.splice(arrayIndex, 1);
	refreshSlides();
}




function editSlide(arrayIndex)
{
	let tempReminderItem = JS_SLIDES_DATA[arrayIndex];
	
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Edit Slide Message", tempReminderItem, 'editSlide('+arrayIndex+')', false);
	return;
	}

	let selectedLanguageCode = inputDialogValue.replace(/"/g, '“');
		JS_SLIDES_DATA[arrayIndex] = selectedLanguageCode;
		refreshSlides();
}




function displayBottomMessagesTable()
{

	clearMessagesTable();

	for(let i = 0; i < JS_BOTTOM_MSGS.length; i++)
	{
		let reminderItem = JS_BOTTOM_MSGS[i];
		if(reminderItem !== "")
		{
			let messageParts = reminderItem.split("|");
			let tableRow = messagesTableBody.insertRow(-1);
			let tableCellAction = tableRow.insertCell(0);
			let tableCellMove = tableRow.insertCell(1);
			let tableCellDay = tableRow.insertCell(2);
			let tableCellMessage = tableRow.insertCell(3);
			let tableCellDelete = tableRow.insertCell(4);

			let azkarLine = "";
			if(messageParts[0] == "1") azkarLine = "checked";
			tableCellAction.innerHTML = "<input type='checkbox' class='settingsCheckboxClass' onchange='toggleMessageStatus(this," + i + ")' " + azkarLine + ">";
			if(i > 0) tableCellMove.innerHTML = "<span class='cursorPointerClass' onclick='moveMessageUp(" + i + ")'>&#8593;</span>";
			tableCellDay.innerHTML = "<span ondblclick='editMessageDay(" + i + ")'>" + messageParts[1] + "</span>";
			tableCellMessage.innerHTML = "<span ondblclick='editMessageText(" + i + ")'>" + messageParts[2] + "</span>";
			tableCellDelete.innerHTML = "<span class='cursorPointerClass' onclick='deleteMessage(" + i + ")'> X </span>";
		}
	}
}




function toggleMessageStatus(languageSpanElement, arrayIndex)
{
	let newStatus = "0";
	if(languageSpanElement.checked) newStatus = "1";
	JS_BOTTOM_MSGS[arrayIndex] = JS_BOTTOM_MSGS[arrayIndex].replace(/^./, newStatus);
	refreshBottomMessages();
}




function moveMessageUp(arrayIndex)
{
	let cityCodePartsArray = JS_BOTTOM_MSGS[arrayIndex - 1];
	JS_BOTTOM_MSGS[arrayIndex - 1] = JS_BOTTOM_MSGS[arrayIndex];
	JS_BOTTOM_MSGS[arrayIndex] = cityCodePartsArray;
	refreshBottomMessages();
}




function deleteMessage(arrayIndex)
{
	JS_BOTTOM_MSGS.splice(arrayIndex, 1);
	refreshBottomMessages();
}




function editMessageText(arrayIndex)
{
	let messageParts = JS_BOTTOM_MSGS[arrayIndex].split("|");
	
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Edit Message", messageParts[2], 'editMessageText('+arrayIndex+')', false);
	return;
	}

		let selectedLanguageCode = inputDialogValue.replace(/"/g, '“');
		messageParts[2] = selectedLanguageCode;
		JS_BOTTOM_MSGS[arrayIndex] = messageParts[0] + "|" + messageParts[1] + "|" + messageParts[2];
		refreshBottomMessages();

}




function editMessageDay(arrayIndex)
{
	let messageParts = JS_BOTTOM_MSGS[arrayIndex].split("|");

	let daySelectionPrompt  = "Choose EXACTLY one of these days. اختر من هذه الأيام  :<br>";
		daySelectionPrompt += "JOMOA . SATURDAY . SUNDAY . MONDAY . TUESDAY . WEDNESDAY . THURSDAY <br><br>";
		daySelectionPrompt += "If you want your message to appear on a special HIJRI date (eg: 27th Ramadan), enter : 09/27 <br><br>";


	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(daySelectionPrompt, messageParts[1] , 'editMessageDay('+arrayIndex+')', true);
	return;
	}

		let selectedLanguageCode = inputDialogValue.replace(/"/g, '“');
		messageParts[1] = selectedLanguageCode;
		JS_BOTTOM_MSGS[arrayIndex] = messageParts[0] + "|" + messageParts[1] + "|" + messageParts[2];
		refreshBottomMessages();

}




function clearMessagesTable()
{
	while(messagesTableBody.hasChildNodes())
	{
		messagesTableBody.removeChild(messagesTableBody.firstChild);
	}
}




function saveBottomMessages()
{
	serializedMessages = ""; 
	for(let i = 0; i < JS_BOTTOM_MSGS.length; i++)
	{
		let tempMessageItem = JS_BOTTOM_MSGS[i];
		if(tempMessageItem !== "") serializedMessages += tempMessageItem + "°°";
	}
	localStorage.setItem('STORAGE_ADMIN_MESSAGES', serializedMessages);
}




function updateMarqueeText()
{
	marqueeText = ""; 

	for(let i = 0; i < JS_BOTTOM_MSGS.length; i++)
	{
		let messageParts = JS_BOTTOM_MSGS[i].split("|");

		if(messageParts[0] == "1")
		{
			if((messageParts[1] == "DAILY") || (messageParts[1] == currentWeekDayName) || (messageParts[1] == hijriMonthDay) || (messageParts[1] == specialEventLabel))
			{
				if(marqueeText !== "") marqueeText += "<span class='messageSeparatorClass'>&bull;</span>";
				marqueeText += messageParts[2];
			}
		}
	}

}


let animationFrameId = null;
let lastTimestamp = null;
let currentTranslateX = 0;
let marqueeDirection = 'right';

let marqueeElement = null;
let marqueeWidth = 0;
let containerWidth = 0;


function setMarqueeDirection(directionParam) 
{
    if (directionParam !== 'left' && directionParam !== 'right') return;
    marqueeDirection = directionParam;
}


function initMarqueePosition(marqueeElement) {
    if (!marqueeElement) return;
    const parentContainer = marqueeElement.parentElement;
    if (!parentContainer) return;


    marqueeElement.style.display = 'inline-block';
    marqueeElement.style.whiteSpace = 'nowrap';
    marqueeElement.style.overflow = 'visible';
    marqueeElement.style.willChange = 'transform';

    parentContainer.style.overflow = 'hidden';

    marqueeWidth = marqueeElement.offsetWidth;
    containerWidth = parentContainer.offsetWidth;

    if (marqueeDirection === 'left') 
    {
        currentTranslateX = containerWidth; 
    } else 
    	{
        currentTranslateX = -marqueeWidth; 
    }

    marqueeElement.style.transform = 'translate3d(' + currentTranslateX + 'px, 0, 0)';
}

function animateMarquee(timestamp) {
    if (lastTimestamp == null) lastTimestamp = timestamp;
    const deltaTime = (timestamp - lastTimestamp) / 1000;
    lastTimestamp = timestamp;

    const directionMultiplier = (marqueeDirection === 'left') ? -1 : 1;
    currentTranslateX += directionMultiplier * JS_DATA.ucMovingMessagesSpeed * deltaTime;

    if (marqueeDirection === 'left') {
        if (currentTranslateX < -marqueeWidth) {
            currentTranslateX = containerWidth;
        }
    } else {
        if (currentTranslateX > containerWidth) {
            currentTranslateX = -marqueeWidth; 
        }
    }

    marqueeElement.style.transform = 'translate3d(' + currentTranslateX + 'px, 0, 0)';
    animationFrameId = requestAnimationFrame(animateMarquee);
}

function startMarqueeAnimation() {
    if (animationFrameId != null) return;
    lastTimestamp = null;
    animationFrameId = requestAnimationFrame(animateMarquee);
}

function stopMarqueeAnimation() {
    if (animationFrameId != null) {
        cancelAnimationFrame(animationFrameId);
        animationFrameId = null;
        lastTimestamp = null;
    }
}



let marqueeTextHorizontalElement = null; 
let marqueeTextVerticalElement = null; 

function updateMarqueeDisplayFunction() 
{


let detectedDirection = "";
let ramadanDaysLeftMessage = "";

	if(marqueeText == "")
	{
		hideElementByIdFunction('hadithDisplayVertical');
		hideElementByIdFunction('hadithDisplayHorizontal');
		hideElementByIdFunction('marqueeContainerVertical');
		hideElementByIdFunction('marqueeContainerHorizontal');

	}
	else 
	{

			if(JS_DATA.ucIsMarqueeOn == 1)
			{
				if( (JS_DATA.ucRamadanDaysLeft == 1) && (ramadanDaysLeftText !== ""))
				ramadanDaysLeftMessage =  JS_eLang.cx_RamadanLeftDays + "&nbsp; (" + ramadanDaysLeftText + ")<span class='messageSeparatorClass'>&bull;</span>";
				
								
								if(isRTLText(marqueeText))
								{
								detectedDirection = 'right';
								}
								else
								{
								detectedDirection = 'left';
								}						
						
						
						if(isHorizontalOrientation)
						{
						showElementFunction('marqueeContainerHorizontal');
						hideElementByIdFunction('hadithDisplayHorizontal');
						
							    marqueeContainerHorizontalElement.innerHTML	= "<div id='marqueeTextHorizontal'></div>";
							    marqueeTextHorizontalElement 				= document.getElementById('marqueeTextHorizontal');
							    marqueeTextHorizontalElement.innerHTML 		= ramadanDaysLeftMessage + marqueeText;
							
							    marqueeElement = marqueeTextHorizontalElement;

							    stopMarqueeAnimation();
							    setMarqueeDirection(detectedDirection);
							    initMarqueePosition(marqueeElement);
							    startMarqueeAnimation();
						}
					else
						{
						showElementFunction('marqueeContainerVertical');
						hideElementByIdFunction('hadithDisplayVertical');
						
							    marqueeContainerVerticalElement.innerHTML	= "<div id='marqueeTextVertical'></div>";
							    marqueeTextVerticalElement 				= document.getElementById('marqueeTextVertical');
							    marqueeTextVerticalElement.innerHTML 		= ramadanDaysLeftMessage + marqueeText;
							
							    marqueeElement = marqueeTextVerticalElement;

							    stopMarqueeAnimation();
							    setMarqueeDirection(detectedDirection);
							    initMarqueePosition(marqueeElement);
							    startMarqueeAnimation();
						}
			
			}
			else
			{

						if(isHorizontalOrientation)
						{
						 stopMarqueeAnimation();
						hideElementByIdFunction('marqueeContainerHorizontal');
						showElementFunction('hadithDisplayHorizontal');
						}
					else
						{
						 stopMarqueeAnimation();
						hideElementByIdFunction('marqueeContainerVertical');
						showElementFunction('hadithDisplayVertical');
						}	

			}
	}




}


function closeModalFunction(languageSpanElement)
{
languageSpanElement.parentNode.parentNode.style.visibility = 'hidden';
}




// HIJRI DATE FROM MILADI, CODE FROM : http://www.vivvo.net/forums/showthread.php?p=40981
function MiladiToHIJRI(myDate, _dek)
{

let jsHoursNow = myDate.getHours();

	d = parseInt(myDate.getDate());
	m = parseInt(myDate.getMonth() + 1);
	y = parseInt(myDate.getFullYear());

	if((y > 1582) || ((y == 1582) && (m > 10)) || ((y == 1582) && (m == 10) && (d > 14)))
	{
		jd = intPart((1461 * (y + 4800 + intPart((m - 14) / 12))) / 4) +
			intPart((367 * (m - 2 - 12 * (intPart((m - 14) / 12)))) / 12) -
			intPart((3 * (intPart((y + 4900 + intPart((m - 14) / 12)) / 100))) / 4) + d - 32075
	}
	else
	{
		jd = 367 * y - intPart((7 * (y + 5001 + intPart((m - 9) / 7))) / 4) + intPart((275 * m) / 9) + d + 1729777;
	}

	JD = jd;


	l = jd - 1948440 + 10632;
	n = intPart((l - 1) / 10631);
	l = l - 10631 * n + 354;
	j = (intPart((10985 - l) / 5316)) * (intPart((50 * l) / 17719)) + (intPart(l / 5670)) * (intPart((43 * l) / 15238));
	l = l - (intPart((30 - j) / 15)) * (intPart((17719 * j) / 50)) - (intPart(j / 16)) * (intPart((15238 * j) / 43)) + 29;

	m = intPart((24 * l) / 709);

	d = l - intPart((709 * m) / 24);

	y = 30 * n + j - 30;

	// DECALAGE_CALC
	d = d + _dek;
	if(d < 1)
	{
		d = 30;
		m--;
	}
	if(d > 30)
	{
		d = 1;
		m++;
	}

	if(m < 1) m = 12;
	if(m > 12) m = 1;

	const isMuharam   = (m == 7);
	const isShaban = (m == 8);
	
	
	isRamadan = (m == 9);
	hijriDateString = "<span class='hijriDateClass'><span id='hijriDaySpan'> " + d + "</span> " + hijriMonthsArray[m - 1] + " </span><span id='hijriYearSpan'>" + y + "</span>";
	hijriDateShort = "<span id='hijriDaySpan'> " + d + "</span> " + hijriMonthsArray[m - 1] + " <span id='hijriYearSpan'>" + y + "</span>";

	hijriDateArray = [d, hijriMonthsArray[m - 1], y];
	isEidAlAdhaPeriod = ((m == 12) && ((d == 9) || (d == 10) || (d == 11) || (d == 12) || (d == 13)));
	isLastDaysRamadan = ((m == 8) && ((d == 29) || (d == 30)));
	isFirstTenDhulHijjah = ((m == 12) && (d >= 1 && d <= 9));
	isLastTenRamadan = ((m == 9) && (d >= 20 && d < 29));
	isEidOrTashreeq = (m == 10 && d == 1) || ((m == 12) && (d >= 10 && d <= 13));

	isWhiteDaysPeriod = (d >= 12 && d <= 14) && ((!isRamadan) && (!isEidAlAdhaPeriod) && (!isLastDaysRamadan));

	let xMM = '0' + m;
	xMM = xMM.slice(-2);
	let xDD = '0' + d;
	xDD = xDD.slice(-2);

	hijriMonthDay = xMM + "/" + xDD;

	isDhulHijjahFirstNine = ((m == 12) && (d >= 1 && d <= 9));
	if(isDhulHijjahFirstNine) specialEventLabel = "10DLHJ";
	else specialEventLabel = "";


	isEidFitrPeriod = ((m == 9) && (d >= 28));
	if( (m == 10) && (d == 1) && (jsHoursNow < 10) ) isEidFitrPeriod = true;
	
	isEidAdhaPeriod = ((m == 12) && (d >= 7 && d <= 9));
	if( (m == 12) && (d == 10) && (jsHoursNow < 10) ) isEidAdhaPeriod = true;

				if(isMuharam)
				{
				ramadanDaysLeftText = " 1 Month," + (30 - d)+" days ";
				}
			else
				if(isShaban)
				{
				ramadanDaysLeftText = (30 - d) + " days ";
				}

}




function intPart(floatNum)
{
	if(floatNum < -0.0000001)
	{
		return Math.ceil(floatNum - 0.0000001)
	}
	return Math.floor(floatNum + 0.0000001)
}




function decrementAthanMinutesFunction(prayerKey)
{

	switch(prayerKey)
	{

		case 'FAJR':
			JS_DATA.ucAthanMinutesFAJR--;
			if(JS_DATA.ucAthanMinutesFAJR < -60)
			{
				JS_DATA.ucAthanMinutesFAJR = -60;
				return;
			}
			athanFajrValueElement.innerHTML = JS_DATA.ucAthanMinutesFAJR;
			saveSettingsToStorageFunction();
			break;

		case 'SHRQ':
			JS_DATA.ucAthanMinutesSHRQ--;
			if(JS_DATA.ucAthanMinutesSHRQ < -60)
			{
				JS_DATA.ucAthanMinutesSHRQ = -60;
				return;
			}
			athanShrqValueElement.innerHTML = JS_DATA.ucAthanMinutesSHRQ;
			saveSettingsToStorageFunction();
			break;

		case 'DOHR':
			JS_DATA.ucAthanMinutesDOHR--;
			if(JS_DATA.ucAthanMinutesDOHR < -60)
			{
				JS_DATA.ucAthanMinutesDOHR = -60;
				return;
			}
			athanDohrValueElement.innerHTML = JS_DATA.ucAthanMinutesDOHR;
			saveSettingsToStorageFunction();
			break;

		case 'ASR':
			JS_DATA.ucAthanMinutesASSR--;
			if(JS_DATA.ucAthanMinutesASSR < -60)
			{
				JS_DATA.ucAthanMinutesASSR = -60;
				return;
			}
			athanAsrValueElement.innerHTML = JS_DATA.ucAthanMinutesASSR;
			saveSettingsToStorageFunction();
			break;

		case 'MAGHRIB':
			JS_DATA.ucAthanMinutesMGRB--;
			if(JS_DATA.ucAthanMinutesMGRB < -60)
			{
				JS_DATA.ucAthanMinutesMGRB = -60;
				return;
			}
			athanMgrbValueElement.innerHTML = JS_DATA.ucAthanMinutesMGRB;
			saveSettingsToStorageFunction();
			break;

		case 'ISHA':
			JS_DATA.ucAthanMinutesISHA--;
			if(JS_DATA.ucAthanMinutesISHA < -60)
			{
				JS_DATA.ucAthanMinutesISHA = -60;
				return;
			}
			athanIshaValueElement.innerHTML = JS_DATA.ucAthanMinutesISHA;
			saveSettingsToStorageFunction();
			break;

		default:
			break;
	}

	calculateAndDisplayTimesFunction();
}




function incrementAthanMinutesFunction(prayerKey)
{

	switch(prayerKey)
	{

		case 'FAJR':
			JS_DATA.ucAthanMinutesFAJR++;
			if(JS_DATA.ucAthanMinutesFAJR > 60)
			{
				JS_DATA.ucAthanMinutesFAJR = 60;
				return;
			}
			athanFajrValueElement.innerHTML = JS_DATA.ucAthanMinutesFAJR;
			saveSettingsToStorageFunction();
			break;

		case 'SHRQ':
			JS_DATA.ucAthanMinutesSHRQ++;
			if(JS_DATA.ucAthanMinutesSHRQ > 60)
			{
				JS_DATA.ucAthanMinutesSHRQ = 60;
				return;
			}
			athanShrqValueElement.innerHTML = JS_DATA.ucAthanMinutesSHRQ;
			saveSettingsToStorageFunction();
			break;

		case 'DOHR':
			JS_DATA.ucAthanMinutesDOHR++;
			if(JS_DATA.ucAthanMinutesDOHR > 60)
			{
				JS_DATA.ucAthanMinutesDOHR = 60;
				return;
			}
			athanDohrValueElement.innerHTML = JS_DATA.ucAthanMinutesDOHR;
			saveSettingsToStorageFunction();
			break;

		case 'ASR':
			JS_DATA.ucAthanMinutesASSR++;
			if(JS_DATA.ucAthanMinutesASSR > 60)
			{
				JS_DATA.ucAthanMinutesASSR = 60;
				return;
			}
			athanAsrValueElement.innerHTML = JS_DATA.ucAthanMinutesASSR;
			saveSettingsToStorageFunction();
			break;

		case 'MAGHRIB':
			JS_DATA.ucAthanMinutesMGRB++;
			if(JS_DATA.ucAthanMinutesMGRB > 60)
			{
				JS_DATA.ucAthanMinutesMGRB = 60;
				return;
			}
			athanMgrbValueElement.innerHTML = JS_DATA.ucAthanMinutesMGRB;
			saveSettingsToStorageFunction();
			break;

		case 'ISHA':
			JS_DATA.ucAthanMinutesISHA++;
			if(JS_DATA.ucAthanMinutesISHA > 60)
			{
				JS_DATA.ucAthanMinutesISHA = 60;
				return;
			}
			athanIshaValueElement.innerHTML = JS_DATA.ucAthanMinutesISHA;
			saveSettingsToStorageFunction();
			break;

		default:
			break;
	}

	calculateAndDisplayTimesFunction();
}




const iqamaFajrValueElement = document.getElementById('iqamaFajrValue');
const iqamaDohaValueElement = document.getElementById('iqamaDohaValue');
const iqamaDohrValueElement = document.getElementById('iqamaDohrValue');
const iqamaAsrValueElement = document.getElementById('iqamaAsrValue');
const iqamaMgrbValueElement = document.getElementById('iqamaMgrbValue');
const iqamaIshaValueElement = document.getElementById('iqamaIshaValue');




function decrementIqamaMinutesFunction(prayerKey)
{

	switch(prayerKey)
	{

		case 'FAJR':
			JS_DATA.ucIqamaFAJR--;
			if(JS_DATA.ucIqamaFAJR < 1)
			{
				JS_DATA.ucIqamaFAJR = 1;
				return;
			}
			iqamaFajrValueElement.innerHTML = JS_DATA.ucIqamaFAJR;
			saveSettingsToStorageFunction();
			break;

		case 'SHRQ':
			JS_DATA.ucIqamaSHRQ--;
			if(JS_DATA.ucIqamaSHRQ < 12)
			{
				JS_DATA.ucIqamaSHRQ = 12;
				return;
			}
			iqamaDohaValueElement.innerHTML = JS_DATA.ucIqamaSHRQ;
			saveSettingsToStorageFunction();
			break;

		case 'DOHR':
			JS_DATA.ucIqamaDOHR--;
			if(JS_DATA.ucIqamaDOHR < 1)
			{
				JS_DATA.ucIqamaDOHR = 1;
				return;
			}
			iqamaDohrValueElement.innerHTML = JS_DATA.ucIqamaDOHR;
			saveSettingsToStorageFunction();
			break;

		case 'ASR':
			JS_DATA.ucIqamaASSR--;
			if(JS_DATA.ucIqamaASSR < 1)
			{
				JS_DATA.ucIqamaASSR = 1;
				return;
			}
			iqamaAsrValueElement.innerHTML = JS_DATA.ucIqamaASSR;
			saveSettingsToStorageFunction();
			break;

		case 'MAGHRIB':
			JS_DATA.ucIqamaMGRB--;
			if(JS_DATA.ucIqamaMGRB < 1)
			{
				JS_DATA.ucIqamaMGRB = 1;
				return;
			}
			iqamaMgrbValueElement.innerHTML = JS_DATA.ucIqamaMGRB;
			saveSettingsToStorageFunction();
			break;

		case 'ISHA':
			JS_DATA.ucIqamaISHA--;
			if(JS_DATA.ucIqamaISHA < 1)
			{
				JS_DATA.ucIqamaISHA = 1;
				return;
			}
			iqamaIshaValueElement.innerHTML = JS_DATA.ucIqamaISHA;
			saveSettingsToStorageFunction();
			break;

		default:
			break;
	}

	updateIqamaTimesDisplayFunction();
}




function incrementIqamaMinutesFunction(prayerKey)
{

	switch(prayerKey)
	{

		case 'FAJR':
			JS_DATA.ucIqamaFAJR++;
			if(JS_DATA.ucIqamaFAJR > 70)
			{
				JS_DATA.ucIqamaFAJR = 70;
				return;
			}
			iqamaFajrValueElement.innerHTML = JS_DATA.ucIqamaFAJR;
			saveSettingsToStorageFunction();
			break;

		case 'SHRQ':
			JS_DATA.ucIqamaSHRQ++;
			if(JS_DATA.ucIqamaSHRQ > 30)
			{
				JS_DATA.ucIqamaSHRQ = 30;
				return;
			}
			iqamaDohaValueElement.innerHTML = JS_DATA.ucIqamaSHRQ;
			saveSettingsToStorageFunction();
			break;

		case 'DOHR':
			JS_DATA.ucIqamaDOHR++;
			if(JS_DATA.ucIqamaDOHR > 70)
			{
				JS_DATA.ucIqamaDOHR = 70;
				return;
			}
			iqamaDohrValueElement.innerHTML = JS_DATA.ucIqamaDOHR;
			saveSettingsToStorageFunction();
			break;

		case 'ASR':
			JS_DATA.ucIqamaASSR++;
			if(JS_DATA.ucIqamaASSR > 70)
			{
				JS_DATA.ucIqamaASSR = 70;
				return;
			}
			iqamaAsrValueElement.innerHTML = JS_DATA.ucIqamaASSR;
			saveSettingsToStorageFunction();
			break;

		case 'MAGHRIB':
			JS_DATA.ucIqamaMGRB++;
			if(JS_DATA.ucIqamaMGRB > 70)
			{
				JS_DATA.ucIqamaMGRB = 70;
				return;
			}
			iqamaMgrbValueElement.innerHTML = JS_DATA.ucIqamaMGRB;
			saveSettingsToStorageFunction();
			break;

		case 'ISHA':
			JS_DATA.ucIqamaISHA++;
			if(JS_DATA.ucIqamaISHA > 70)
			{
				JS_DATA.ucIqamaISHA = 70;
				return;
			}
			iqamaIshaValueElement.innerHTML = JS_DATA.ucIqamaISHA;
			saveSettingsToStorageFunction();
			break;

		default:
			break;
	}

	updateIqamaTimesDisplayFunction();
}





function decrementDimmerBeforeFunction()
{
	JS_DATA.ucJomoaDimmBefore--;
	if(JS_DATA.ucJomoaDimmBefore < 2)
	{
		JS_DATA.ucJomoaDimmBefore = 2;
		return;
	}
	dimmerBeforeValueElement.innerHTML = JS_DATA.ucJomoaDimmBefore;
	saveSettingsToStorageFunction();
}




function incrementDimmerBeforeFunction()
{
	JS_DATA.ucJomoaDimmBefore++;
	if(JS_DATA.ucJomoaDimmBefore > 120)
	{
		JS_DATA.ucJomoaDimmBefore = 120;
		return;
	}
	dimmerBeforeValueElement.innerHTML = JS_DATA.ucJomoaDimmBefore;
	saveSettingsToStorageFunction();
}




function decrementDimmerAfterFunction()
{
	JS_DATA.ucJomoaDimmAfter--;
	if(JS_DATA.ucJomoaDimmAfter < (JS_DATA.ucJomoaDimmBefore + 1))
	{
		JS_DATA.ucJomoaDimmAfter = (JS_DATA.ucJomoaDimmBefore + 1);
		return;
	}
	dimmerAfterValueElement.innerHTML = JS_DATA.ucJomoaDimmAfter;
	saveSettingsToStorageFunction();
}




function incrementDimmerAfterFunction()
{
	JS_DATA.ucJomoaDimmAfter++;
	if(JS_DATA.ucJomoaDimmAfter > 120)
	{
		JS_DATA.ucJomoaDimmAfter = 120;
		return;
	}
	dimmerAfterValueElement.innerHTML = JS_DATA.ucJomoaDimmAfter;
	saveSettingsToStorageFunction();
}




function showMessagePopupFunction(popupMessage, popupDurationMinutes)
{
	messageDisplayVerticalElement.innerHTML = popupMessage;
	messageDisplayHorizontalElement.innerHTML = popupMessage;
	let popupDurationMs = popupDurationMinutes;
	showElementFunction('messageDisplayContainerVertical');
	setTimeout("hideElementByIdFunction('messageDisplayContainerVertical')", (popupDurationMs * 60000));
	hideElementByIdFunction('ayaDisplayContainerVertical');
	setTimeout("showElementFunction('ayaDisplayContainerVertical')", (popupDurationMs * 60000));

	showElementFunction('messageDisplayContainerHorizontal');
	setTimeout("hideElementByIdFunction('messageDisplayContainerHorizontal')", (popupDurationMs * 60000));
	hideElementByIdFunction('ayaDisplayContainerHorizontal');
	setTimeout("showElementFunction('ayaDisplayContainerHorizontal')", (popupDurationMs * 60000));
}



function showStaticMessageFunction(popupMessage, popupDurationMinutes)
{
isHadithVisible = (hadithDisplayVerticalElement.style.visibility == 'visible'); 

staticMessageVerticalElement.innerHTML = popupMessage;
staticMessageHorizontalElement.innerHTML = popupMessage;
let popupDurationMs = popupDurationMinutes;
	
showElementFunction('staticMessageDisplayVertical'); setTimeout("hideElementByIdFunction('staticMessageDisplayVertical')", (popupDurationMs * 60000));
showElementFunction('staticMessageDisplayHorizontal'); setTimeout("hideElementByIdFunction('staticMessageDisplayHorizontal')", (popupDurationMs * 60000));

hideElementByIdFunction('hadithDisplayVertical');
hideElementByIdFunction('hadithDisplayHorizontal');
hideElementByIdFunction('marqueeContainerVertical');
hideElementByIdFunction('marqueeContainerHorizontal');

	if(isHadithVisible)
	{
	setTimeout("showElementFunction('hadithDisplayVertical')", (popupDurationMs * 60000));
	setTimeout("showElementFunction('hadithDisplayHorizontal')", (popupDurationMs * 60000));
	}
else
	{
	setTimeout("showElementFunction('marqueeContainerVertical')", (popupDurationMs * 60000));
	setTimeout("showElementFunction('marqueeContainerHorizontal')", (popupDurationMs * 60000));
	}
	
}

function updateAthanIqamaDisplayFunction()
{

	athanFajrValueElement.innerHTML = JS_DATA.ucAthanMinutesFAJR;
	athanShrqValueElement.innerHTML = JS_DATA.ucAthanMinutesSHRQ
	athanDohrValueElement.innerHTML = JS_DATA.ucAthanMinutesDOHR;
	athanAsrValueElement.innerHTML = JS_DATA.ucAthanMinutesASSR;
	athanMgrbValueElement.innerHTML = JS_DATA.ucAthanMinutesMGRB;
	athanIshaValueElement.innerHTML = JS_DATA.ucAthanMinutesISHA;

	iqamaFajrValueElement.innerHTML = JS_DATA.ucIqamaFAJR;
	iqamaDohaValueElement.innerHTML = JS_DATA.ucIqamaSHRQ;
	iqamaDohrValueElement.innerHTML = JS_DATA.ucIqamaDOHR;
	iqamaAsrValueElement.innerHTML = JS_DATA.ucIqamaASSR;
	iqamaMgrbValueElement.innerHTML = JS_DATA.ucIqamaMGRB;
	iqamaIshaValueElement.innerHTML = JS_DATA.ucIqamaISHA;

	durationFajrValueElement.innerHTML = JS_DATA.ucPrayDurationFAJR;
	durationDohrValueElement.innerHTML = JS_DATA.ucPrayDurationDOHR;
	durationAsrValueElement.innerHTML = JS_DATA.ucPrayDurationASSR;
	durationMgrbValueElement.innerHTML = JS_DATA.ucPrayDurationMGRB;
	durationIshaValueElement.innerHTML = JS_DATA.ucPrayDurationISHA;

}




function toggleArabicDigitsFunction()
{
	JS_DATA.ucIsArabicDigits = +(!JS_DATA.ucIsArabicDigits);
	saveSettingsToStorageFunction();
	applyArabicDigits(JS_DATA.ucIsArabicDigits,true);
	calculateAndDisplayTimesFunction();
	refreshWeatherDisplay();
}



function toggleUseImportedTimesFunction()
{
	JS_DATA.ucUseImportedTimes = +(!JS_DATA.ucUseImportedTimes);
	saveSettingsToStorageFunction();
	updateUseImportedTimesCheckbox();
}


function updateUseImportedTimesCheckbox()
{
document.getElementById('useImportedTimesCheckbox').checked = (JS_DATA.ucUseImportedTimes == 1);
}


function toggleHrNamesMiddleFunction()
{
if(isBigCounterActive) return;

		JS_DATA.ucHrNamesInMiddle = +(!JS_DATA.ucHrNamesInMiddle);
	saveSettingsToStorageFunction();
	updateHrNamesPositionFunction(true);
}


function updateHrNamesPositionFunction(refreshIqamaDisplay)
{
document.getElementById('middleSalatNamesCheckbox').checked = (JS_DATA.ucHrNamesInMiddle == 1);

	let hrIqamaTopVariant = '13%';
	let hrIqamaBottomVariant = '71%';
	if(JS_DATA.ucIqamaFullTimes == 1)
	{
		hrIqamaTopVariant = '9%';
		hrIqamaBottomVariant = '67%';
	}


	if(JS_DATA.ucHrNamesInMiddle == 1)
	{
		rootElement.style.setProperty('--jnamTOP', '36%');
		rootElement.style.setProperty('--j6hourTOP', '64%');
		rootElement.style.setProperty('--j6iqamaTOP', hrIqamaTopVariant);
		rootElement.style.setProperty('--j5hourTOP', '60%');
		rootElement.style.setProperty('--j5iqamaTOP', hrIqamaTopVariant);
		rootElement.style.setProperty('--mt7', '30%');
	}
	else
	{
		rootElement.style.setProperty('--jnamTOP', '4%');
		rootElement.style.setProperty('--j6hourTOP', '35.5%');
		rootElement.style.setProperty('--j6iqamaTOP', hrIqamaBottomVariant);
		rootElement.style.setProperty('--j5hourTOP', '31.7%');
		rootElement.style.setProperty('--j5iqamaTOP', hrIqamaBottomVariant);
		rootElement.style.setProperty('--mt7', '1%');
	}

	if(refreshIqamaDisplay) updateIqamaTimesDisplayFunction();
}







function updateStorageInfoFunction()
{

const storageInfoElement = document.getElementById('storageInfoDisplay');

	try
	{
		let totalStorageSize = 0;
		for (let i = 0; i < localStorage.length; i++)
		{
			const storageKeyItem		= localStorage.key(i);
			const storageItemValue	= localStorage.getItem(storageKeyItem);
			totalStorageSize += storageKeyItem.length + storageItemValue.length;
		}
		const storageSizeMB	= (totalStorageSize / 1024 / 1024).toFixed(2);
		const storageUsagePercent	= ((totalStorageSize / (5 * 1024 * 1024)) * 100).toFixed(1);
		
		storageInfoElement.innerHTML = 'Storage : ' + storageUsagePercent +'%  - ' + storageSizeMB + 'MB '+ 'of 5MB';

	}
	catch (error)
	{storageInfoElement.innerHTML = 'Error checking storage:' + error;}
}
	


function toggleUserFilesFunction()
{
	JS_DATA.ucUserFilesACTIVE = +(!JS_DATA.ucUserFilesACTIVE);
	saveSettingsToStorageFunction();
	updateUserFilesSectionFunction(0);
	applyThemeFunction(true);
}

function updateUserFilesSectionFunction(loadImagesFromStorage)
{
document.getElementById('userFilesActiveCheckbox').checked = (JS_DATA.ucUserFilesACTIVE == 1);

		if(loadImagesFromStorage)
		{
		updateStorageInfoFunction();
		userUploadedImages[0]	= localStorage.getItem(storageKeysArray[0]);
		userUploadedImages[1]	= localStorage.getItem(storageKeysArray[1]);
		
			if(userUploadedImages[0]) document.getElementById('cjpf0').src = userUploadedImages[0];
			if(userUploadedImages[1]) document.getElementById('cjpf1').src = userUploadedImages[1];
		}

}





function toggleMarqueeFunction()
{
		JS_DATA.ucIsMarqueeOn = +(!JS_DATA.ucIsMarqueeOn);
	saveSettingsToStorageFunction();
	updateMarqueeCheckboxFunction();
}

function updateMarqueeCheckboxFunction()
{
document.getElementById('marqueeActiveCheckbox').checked = (JS_DATA.ucIsMarqueeOn == 1);
updateMarqueeDisplayFunction();
}





function toggleBlackScreenClockFunction()
{
		JS_DATA.ucBlackScreenShowsClock = +(!JS_DATA.ucBlackScreenShowsClock);
	saveSettingsToStorageFunction();
	updateBlackScreenClockCheckboxFunction();
}

function updateBlackScreenClockCheckboxFunction()
{
document.getElementById('blackScreenShowClockCheckbox').checked = (JS_DATA.ucBlackScreenShowsClock == 1);
if(JS_DATA.ucBlackScreenShowsClock == 1) rootElement.style.setProperty('--BsCLock500', 997); else rootElement.style.setProperty('--BsCLock500', 500);
}




function toggleBlackScreenDateFunction()
{
		JS_DATA.ucBlackScreenShowsDate = +(!JS_DATA.ucBlackScreenShowsDate);
	saveSettingsToStorageFunction();
	updateBlackScreenDateCheckboxFunction();
}

function updateBlackScreenDateCheckboxFunction()
{
document.getElementById('blackScreenShowDateCheckbox').checked = (JS_DATA.ucBlackScreenShowsDate == 1);
if(JS_DATA.ucBlackScreenShowsDate == 1) rootElement.style.setProperty('--BsDate500', 997); else rootElement.style.setProperty('--BsDate500', 500);
}







function toggleDohaScreenSaverFunction()
{
		JS_DATA.ucDohaScreenSaver = +(!JS_DATA.ucDohaScreenSaver);
	saveSettingsToStorageFunction();
	updateDohaScreenSaverCheckboxFunction();
}


function updateDohaScreenSaverCheckboxFunction()
{
document.getElementById('dohaScreenSaverCheckbox').checked = (JS_DATA.ucDohaScreenSaver == 1);
}

function toggleRamadanDaysLeftFunction()
{
	JS_DATA.ucRamadanDaysLeft = +(!JS_DATA.ucRamadanDaysLeft);
	saveSettingsToStorageFunction();
	ramadanDaysLeftCheckboxElement.checked = (JS_DATA.ucRamadanDaysLeft == 1);
}




function toggleIshaScreenSaverFunction()
{
		JS_DATA.ucIshaScreenSaver = +(!JS_DATA.ucIshaScreenSaver);
	saveSettingsToStorageFunction();
	updateIshaScreenSaverCheckboxFunction();
}


function updateIshaScreenSaverCheckboxFunction()
{
document.getElementById('ishaScreenSaverCheckbox').checked = (JS_DATA.ucIshaScreenSaver == 1);
}




function saveSettingsToStorageFunction()
{
localStorage.setItem('JS_DATA', JSON.stringify(JS_DATA));
}




function toggleVrNamesMiddleFunction()
{
		JS_DATA.ucVrNamesInMiddle = +(!JS_DATA.ucVrNamesInMiddle);
	saveSettingsToStorageFunction();
	updateVrNamesPositionFunction();
}




function updateVrNamesPositionFunction()
{
	document.getElementById('middleVrNamesCheckbox').checked = (JS_DATA.ucVrNamesInMiddle == 1);
	if(JS_DATA.ucVrNamesInMiddle == 1) prayerTimesContainerVerticalElement.className = 'middleNamesVrClass';
	else prayerTimesContainerVerticalElement.className = '';
}




function toggleIqamaScreenFunction()
{
		JS_DATA.ucShowIqamaScreen = +(!JS_DATA.ucShowIqamaScreen);
	saveSettingsToStorageFunction();
	updateShowIqamaScreenCheckboxFunction();
}

function updateShowIqamaScreenCheckboxFunction()
{
	document.getElementById('showIqamaScreenCheckbox').checked = (JS_DATA.ucShowIqamaScreen == 1);
}





function toggleAzanScreenFunction()
{
		JS_DATA.ucShowAzanWindow = +(!JS_DATA.ucShowAzanWindow);
	saveSettingsToStorageFunction();
	updateShowAzanScreenCheckboxFunction();
}

function updateShowAzanScreenCheckboxFunction()
{
	document.getElementById('showAzanScreenCheckbox').checked = (JS_DATA.ucShowAzanWindow == 1);
}





function toggleBigNextPrayCounterFunction()
{
if(isBigCounterActive) return;
		JS_DATA.ucUseBigNextPrayCounter = +(!JS_DATA.ucUseBigNextPrayCounter);
	saveSettingsToStorageFunction();
	updateBigNextPrayCounterFunction();
}


function updateBigNextPrayCounterFunction() 
{
document.getElementById('bigNextPrayCounterCheckbox').checked = (JS_DATA.ucUseBigNextPrayCounter == 1);
			
			if(isHorizontalOrientation)
			{
			var hrDiffBottom 	= '55%';
			var hrDiffRight 	= '2.5%';
			var nextPrayerFontSize 	= '5.1'+fontSizeUnit;
			var nextPrayerContainerSize 	= '16'+fontSizeUnit;
			

					
				if(JS_DATA.ucUseBigNextPrayCounter == 1) 
				{
				hrDiffBottom 	= '50.7%';
				hrDiffRight 	= '0.75%';
				nextPrayerFontSize 	= '7.1'+fontSizeUnit;
				nextPrayerContainerSize 	= '20.5'+fontSizeUnit;
				}

			
		
			nextPrayerCounterHorizontalElement.style.height	= nextPrayerContainerSize;
			nextPrayerCounterHorizontalElement.style.width		= nextPrayerContainerSize;
			nextPrayerTimeDisplayHorizontalElement.style.fontSize 	= nextPrayerFontSize;

			rootElement.style.setProperty('--HRdiffBtm', hrDiffBottom);
			rootElement.style.setProperty('--HRdiffRight', hrDiffRight);
			}


}


		


function toggleDatePositionFunction()
{
		JS_DATA.ucDateUpRightInHR = +(!JS_DATA.ucDateUpRightInHR);
	saveSettingsToStorageFunction();
	updateDatePositionHrFunction();
}



let weatherClickCounter = 0;
function resetAppOnMultipleClicks()
{
	weatherClickCounter++;
	if(weatherClickCounter > 6) 		
	{
	weatherClickCounter = 0;
	localStorage.clear();
	restartApplicationReload();
	}
}


function updateDatePositionHrFunction()
{
	document.getElementById('dateUpRightHrCheckbox').checked = (JS_DATA.ucDateUpRightInHR == 1);

	if(JS_DATA.ucDateUpRightInHR == 1)
	{
		// ① dateSeparator ET la classe CSS sont définis AVANT updateDateDisplay()
		//   → la date est rendue avec le bon séparateur " . " et la bonne classe (width:auto)
		//   → indispensable pour que getBoundingClientRect() retourne la vraie largeur de la date.
		dateSeparator = " . ";
		dateDisplayHorizontalElement.className = 'dateDisplayClass';
		mosqueNameHorizontalElement.className  = 'mosqueNameClassHorizontal';
		mosqueNameHorizontalElement.style.left  = '';   // réinitialiser les styles inline
		mosqueNameHorizontalElement.style.width = '';
		updateDateDisplay();                            // contenu rendu avec le bon séparateur

		dohaTimeHorizontalElement.style.top = '10.3%';
		weatherWidgetHorizontalElement.className = 'weatherWidgetHrDateRightClass';

		// ② Mesure différée via requestAnimationFrame :
		//   le navigateur a eu le temps de peindre la date avec son vrai contenu
		//   → getBoundingClientRect() retourne des coordonnées pixel fiables.
		requestAnimationFrame(function() {
			const _logo      = document.getElementById('appLogoImageHorizontal');
			const _logoRight = _logo.offsetLeft + _logo.offsetWidth;   // bord droit réel du logo
			const _dateRect  = dateDisplayHorizontalElement.getBoundingClientRect();
			const _gap       = 8;
			// Garde : si la date n'a pas encore de largeur réelle, on abandonne cette frame
			// (le prochain appel de la fonction ou le tick d'horloge corrigera la position).
			if (_dateRect.width < 50) return;
			mosqueNameHorizontalElement.style.left  = (_logoRight + _gap) + 'px';
			mosqueNameHorizontalElement.style.width = Math.max(0, _dateRect.left - _logoRight - _gap * 2) + 'px';
		});
	}
	else
	{
		dateSeparator = "<br>";                          // séparateur défini AVANT updateDateDisplay()
		dateDisplayHorizontalElement.className = 'dateDisplayHrClass';
		mosqueNameHorizontalElement.className  = 'mosqueNameHrClass';
		mosqueNameHorizontalElement.style.left  = '';   // laisser le CSS left:5% s'appliquer
		mosqueNameHorizontalElement.style.width = '90%';
		updateDateDisplay();
		dohaTimeHorizontalElement.style.top = '2.5%';
		weatherWidgetHorizontalElement.className = 'weatherWidgetClass';
		updateEidMessageVisibility();
	}

	updateIqamaTimesDisplayFunction();
}




function toggleVerifyInternetFunction()
{
		JS_DATA.ucVerifyInternet = +(!JS_DATA.ucVerifyInternet);
	saveSettingsToStorageFunction();
	updateVerifyInternetCheckboxFunction();
}


function updateVerifyInternetCheckboxFunction()
{
	document.getElementById('verifyInternetCheckbox').checked = (JS_DATA.ucVerifyInternet == 1);
	checkInternetConnectionFunction();
	if(JS_DATA.ucVerifyInternet == 1) 
	{ showElementFunction('internetStatusIndicatorHorizontal'); showElementFunction('internetStatusIndicatorVertical'); }
	else
	{ hideElementByIdFunction('internetStatusIndicatorHorizontal'); hideElementByIdFunction('internetStatusIndicatorVertical'); }
}



function toggleTimesBgShadowsFunction()
{
		JS_DATA.ucTimesBgShadows = +(!JS_DATA.ucTimesBgShadows);
	saveSettingsToStorageFunction();
	updateTimesBgShadowsFunction();
}


function updateTimesBgShadowsFunction()
{

	document.getElementById('timesBgShadowsCheckbox').checked = (JS_DATA.ucTimesBgShadows == 1);
	if(JS_DATA.ucTimesBgShadows == 1) 
	{
	rootElement.style.setProperty('--BgOnOff', '100% 100%');
	rootElement.style.setProperty('--VRBgOnOff', 'url(./images/xbg.png)');
	}
	else 
		{
		rootElement.style.setProperty('--BgOnOff', '0 0');
		rootElement.style.setProperty('--VRBgOnOff', 'url()');
		}

}


function toggleSemiTransparentBgsFunction()
{
		JS_DATA.ucSemiTransparentBgs = +(!JS_DATA.ucSemiTransparentBgs);
	saveSettingsToStorageFunction();
	updateSemiTransparentBgsFunction();
}


function updateSemiTransparentBgsFunction()
{
	document.getElementById('semiTransparentBgsCheckbox').checked = (JS_DATA.ucSemiTransparentBgs == 1);
	if(JS_DATA.ucSemiTransparentBgs == 1) rootElement.style.setProperty('--BgsTranspa', '0.89');
	else rootElement.style.setProperty('--BgsTranspa', '1');
}





function toggleCounterColorAlertFunction()
{
		JS_DATA.ucCounterColorAlert = +(!JS_DATA.ucCounterColorAlert);
	saveSettingsToStorageFunction();
	updateCounterColorAlertCheckboxFunction();
}


function updateCounterColorAlertCheckboxFunction()
{
	document.getElementById('counterColorAlertCheckbox').checked = (JS_DATA.ucCounterColorAlert == 1);
}




function toggleIqamaHadithFunction()
{
		JS_DATA.ucIqamaHadith = +(!JS_DATA.ucIqamaHadith);
	saveSettingsToStorageFunction();
	updateIqamaHadithCheckboxFunction();
}

function updateIqamaHadithCheckboxFunction()
{
	document.getElementById('iqamaHadithCheckbox').checked = (JS_DATA.ucIqamaHadith == 1);
}




function toggleAlertLastMinuteFunction()
{
	JS_DATA.ucAlertLastMinute = +(!JS_DATA.ucAlertLastMinute);
	saveSettingsToStorageFunction();
	updateAlertLastMinuteCheckboxFunction();
}

function updateAlertLastMinuteCheckboxFunction()
{
	document.getElementById('alertLastMinuteCheckbox').checked = (JS_DATA.ucAlertLastMinute == 1);
}



function toggleEidFitrFunction()
{
		JS_DATA.ucActivateEidFITR = +(!JS_DATA.ucActivateEidFITR);
	saveSettingsToStorageFunction();
	updateEidFitrCheckboxFunction();
}

function updateEidFitrCheckboxFunction()
{
	document.getElementById('eidFitrCheckbox').checked = (JS_DATA.ucActivateEidFITR == 1);
}



function toggleEidAdhaFunction()
{
		JS_DATA.ucActivateEidADHA = +(!JS_DATA.ucActivateEidADHA);
	saveSettingsToStorageFunction();
	updateEidAdhaCheckboxFunction();
}

function updateEidAdhaCheckboxFunction()
{
	document.getElementById('eidAdhaCheckbox').checked = (JS_DATA.ucActivateEidADHA == 1);
}





function toggleAzkarSabahFunction()
{
		JS_DATA.ucAzkarSabahOn = +(!JS_DATA.ucAzkarSabahOn);
	saveSettingsToStorageFunction();
	updateAzkarOnSettingsFunction();
}




function toggleAzkarAsrFunction()
{
		JS_DATA.ucAzkarAsrOn = +(!JS_DATA.ucAzkarAsrOn);
	saveSettingsToStorageFunction();
	updateAzkarOnSettingsFunction();
}




function toggleAzkarMaghribFunction()
{
		JS_DATA.ucAzkarMaghribOn = +(!JS_DATA.ucAzkarMaghribOn);
	saveSettingsToStorageFunction();
	updateAzkarOnSettingsFunction();
}




function toggleAzkarIshaFunction()
{
		JS_DATA.ucAzkarIshaOn = +(!JS_DATA.ucAzkarIshaOn);
	saveSettingsToStorageFunction();
	updateAzkarOnSettingsFunction();
}




function updateAzkarOnSettingsFunction()
{
	document.getElementById('azkarSabahCheckbox').checked = (JS_DATA.ucAzkarSabahOn == 1);
	document.getElementById('azkarAsrCheckbox').checked = (JS_DATA.ucAzkarAsrOn == 1);
	document.getElementById('azkarMaghribCheckbox').checked = (JS_DATA.ucAzkarMaghribOn == 1);
	document.getElementById('azkarIshaCheckbox').checked = (JS_DATA.ucAzkarIshaOn == 1);
}


function redirectToAudioHandlerFunction(jxfile)
{
location.href = "https://tawkit_audio?xfile="+jxfile;
}






function toggleFullScreenCounterFunction()
{
		JS_DATA.ucFullScreenCounter = +(!JS_DATA.ucFullScreenCounter);
	saveSettingsToStorageFunction();
	updateFullScreenCounterCheckboxFunction();
}

function updateFullScreenCounterCheckboxFunction()
{
	document.getElementById('fullScreenCounterCheckbox').checked = (JS_DATA.ucFullScreenCounter == 1);
}







function toggleLastMinuteCounterFunction()
{
		JS_DATA.ucCounterLastMinute = +(!JS_DATA.ucCounterLastMinute);
	saveSettingsToStorageFunction();
	updateLastMinuteCounterCheckboxFunction();
}

function updateLastMinuteCounterCheckboxFunction()
{
	document.getElementById('lastMinuteCounterCheckbox').checked = (JS_DATA.ucCounterLastMinute == 1);
}






function toggleJomoaAzanFunction()
{
		JS_DATA.ucActivateJomoaAzan = +(!JS_DATA.ucActivateJomoaAzan);
	saveSettingsToStorageFunction();
	updateJomoaAzanCheckboxFunction();
}


function updateJomoaAzanCheckboxFunction()
{
	document.getElementById('jomoaAzanCheckbox').checked = (JS_DATA.ucActivateJomoaAzan == 1);
}






function toggleJomoaOnHrScreenFunction()
{
		JS_DATA.ucJomoaOnHRscreen = +(!JS_DATA.ucJomoaOnHRscreen);
	saveSettingsToStorageFunction();
	applyHrLayoutFunction();
}




function toggleHr5BoxesFunction()
{
if(isBigCounterActive) return;
		JS_DATA.ucHr5BoxesOnly = +(!JS_DATA.ucHr5BoxesOnly);
	saveSettingsToStorageFunction();
	applyHrLayoutFunction();
}






function updateJomoaLogoDisplayFunction()
{
		if(JS_DATA.ucJomoaOnHRscreen == 1)
		{
			hideElementByIdFunction('appLogoImageHorizontal');
			showElementFunction('jomoaCounterHorizontal');
		}
		else
		{
			showElementFunction('appLogoImageHorizontal');
			hideElementByIdFunction('jomoaCounterHorizontal');
		}
}


function applyHrLayoutFunction()
{
	document.getElementById('fiveBoxesOnlyCheckbox').checked   = (JS_DATA.ucHr5BoxesOnly == 1);
	document.getElementById('jomoaOnHrCheckbox').checked = (JS_DATA.ucJomoaOnHRscreen == 1);

	if(JS_DATA.ucHr5BoxesOnly == 1)
	{
		prayerCellShrqHorizontalElement.style.display = 'none';
		prayerTimesTableHorizontalElement.style.borderSpacing = '2.5'+fontSizeUnit;
		prayerTimesContainerHorizontalElement.style.top = '58.3%';
		showElementFunction('dohaTimeDisplayHorizontal');
		updateJomoaLogoDisplayFunction();
		prayerTimesTableHorizontalElement.className = 'fivePrayersTableClass';
	}
	else
	{
		prayerCellShrqHorizontalElement.style.display = '';
		prayerTimesTableHorizontalElement.style.borderSpacing = '1.0'+fontSizeUnit;
		prayerTimesContainerHorizontalElement.style.top = '61.0%';
		hideElementByIdFunction('dohaTimeDisplayHorizontal');
		updateJomoaLogoDisplayFunction();
		prayerTimesTableHorizontalElement.className = 'sixPrayersTableClass';
	}

	updateIqamaTimesDisplayFunction();
}





function toggleAddZeroAmPmFunction()
{
		JS_DATA.ucAddZeroToAMPM = +(!JS_DATA.ucAddZeroToAMPM);
	saveSettingsToStorageFunction();
	updateAddZeroAmPmCheckboxFunction();
	calculateAndDisplayTimesFunction(); 
}




function updateAddZeroAmPmCheckboxFunction()
{
document.getElementById('addZeroAmPmCheckbox').checked = (JS_DATA.ucAddZeroToAMPM == 1);
}






function toggleHomeModeFunction()
{
		JS_DATA.ucIsForHome = +(!JS_DATA.ucIsForHome);
	saveSettingsToStorageFunction();
	updateHomeModeLayoutFunction();
}


function updateHomeModeLayoutFunction()
{
document.getElementById('hideIqamatCheckbox').checked = (JS_DATA.ucIsForHome == 1);

	if(JS_DATA.ucIsForHome == 1)
	{
		rootElement.style.setProperty('--IQAMAT', 'hidden');
		if(isRTL) prayerTimesContainerVerticalElement.style.left = '-8%';
		else prayerTimesContainerVerticalElement.style.left = '27%';
	}
	else
	{
		prayerTimesContainerVerticalElement.style.left = '8%';
		rootElement.style.setProperty('--IQAMAT', 'visible');
	}

}




function applyArabicDigits(useArabicDigits,refreshClockDisplay)
{
arabicDigitsCheckboxElement.checked = useArabicDigits;

	if(useArabicDigits)
	{
			if(languagesWithNoArabicDigits.indexOf(JS_DATA.ucLangNOW) > 0)
			{
			rootElement.style.setProperty('--fnDIGITS', 'var(--csvar_mainFONT)');
			rootElement.style.setProperty('--fnCLOKEN', 'var(--csvar_mainFONT)');
			}			
			else
			{
			rootElement.style.setProperty('--fnDIGITS', 'CORDOBA');
			rootElement.style.setProperty('--fnCLOKEN', 'CORDOBA');
			}
	}
	else
	{
	rootElement.style.setProperty('--fnDIGITS', JS_DATA.ucTimesFont);
	rootElement.style.setProperty('--fnCLOKEN', JS_DATA.ucClockFont);
	}

if(refreshClockDisplay) updateTimeAndPrayersFunction(false);
}


let dohaTimeDisplayString = "---";
let dateSeparator = "<br>";

function updateIqamaTimesDisplayFunction()
{

	let iqamaSymbol = '’';
	counterForDohaTimeDisplay++;
	dohaTimeDisplayString = convertToArabicDigitsFunction(convertTo12HourFormat(formatPrayerTimeDiff(shuruqTimeInMinutes + parseInt(JS_DATA.ucIqamaSHRQ))));

	               let dohaDisplayText = JS_eLang.cx_DOHA   + " <span class='dohaTimeClass'>" + dohaTimeDisplayString + "</span>";
	dohaTimeHorizontalElement.innerHTML = prayerNameShrqVerticalSpanElement.innerHTML + " <span class='dohaTimeClass'>" + azanTimeShrqVerticalSpanElement.innerHTML + "</span>" + dateSeparator + dohaDisplayText;
	dohaTimeVerticalElement.innerHTML	= prayerNameShrqVerticalSpanElement.innerHTML + " <span class='dohaTimeClass'>" + azanTimeShrqVerticalSpanElement.innerHTML + "</span><br>" + dohaDisplayText;

	let hrCellBackgroundUrl = 'url(./images/hrCellBgc.png)';

	if(JS_DATA.ucIqamaFullTimes == 1)
	{
		iqamaTimeFajrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(formatPrayerTimeDiff(fajrTimeInMinutes + parseInt(JS_DATA.ucIqamaFAJR))));
		iqamaTimeShrqVerticalSpanElement.innerHTML = dohaTimeDisplayString;
		iqamaTimeDohrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(formatPrayerTimeDiff(dohrTimeInMinutes + parseInt(JS_DATA.ucIqamaDOHR))));
		iqamaTimeAsrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(formatPrayerTimeDiff(asrTimeInMinutes + parseInt(JS_DATA.ucIqamaASSR))));
		iqamaTimeMgrbVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(formatPrayerTimeDiff(maghribTimeInMinutes + parseInt(JS_DATA.ucIqamaMGRB))));
		iqamaTimeIshaVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(formatPrayerTimeDiff(ishaTimeInMinutes + parseInt(JS_DATA.ucIqamaISHA))));
		rootElement.style.setProperty('--j6iqamaSZ', '4.5'+fontSizeUnit);
		rootElement.style.setProperty('--j5iqamaSZ', '5.2'+fontSizeUnit);
	}
	else
	{
		iqamaTimeFajrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(JS_DATA.ucIqamaFAJR) + iqamaSymbol;
		iqamaTimeShrqVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(JS_DATA.ucIqamaSHRQ) + iqamaSymbol;
		iqamaTimeDohrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(JS_DATA.ucIqamaDOHR) + iqamaSymbol;
		iqamaTimeAsrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(JS_DATA.ucIqamaASSR) + iqamaSymbol;
		iqamaTimeMgrbVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(JS_DATA.ucIqamaMGRB) + iqamaSymbol;
		iqamaTimeIshaVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(JS_DATA.ucIqamaISHA) + iqamaSymbol;
		rootElement.style.setProperty('--j6iqamaSZ', '3.1'+fontSizeUnit);
		rootElement.style.setProperty('--j5iqamaSZ', '3.5'+fontSizeUnit);

		if(JS_DATA.ucHrNamesInMiddle == 1) hrCellBackgroundUrl = 'url(./images/hrCellBg9.png)';
	}


	updateHrNamesPositionFunction(false);


	rootElement.style.setProperty('--HRBG', hrCellBackgroundUrl);


	if((JS_DATA.ucFixedIqamaFAJR !== "") && (!JS_DATA.ucWcsvIsActive == 1))
	{
		iqamaTimeFajrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(JS_DATA.ucFixedIqamaFAJR));
	}

	if((JS_DATA.ucFixedIqamaDOHR !== "") && (!JS_DATA.ucWcsvIsActive == 1))
	{
		iqamaTimeDohrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(JS_DATA.ucFixedIqamaDOHR));
	}

	if((JS_DATA.ucFixedIqamaASSR !== "") && (!JS_DATA.ucWcsvIsActive == 1))
	{
		iqamaTimeAsrVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(JS_DATA.ucFixedIqamaASSR));
	}

	if((JS_DATA.ucFixedIqamaISHA !== "") && (!JS_DATA.ucWcsvIsActive == 1))
	{
		iqamaTimeIshaVerticalSpanElement.innerHTML = convertToArabicDigitsFunction(convertTo12HourFormat(JS_DATA.ucFixedIqamaISHA));
	}


	if(isFriday)
	{
		iqamaTimeDohrVerticalSpanElement.innerHTML  = "···&nbsp;";
	}


	iqamaTimeFajrHorizontalSpanElement.innerHTML = iqamaTimeFajrVerticalSpanElement.innerHTML;
	iqamaTimeShrqHorizontalSpanElement.innerHTML = iqamaTimeShrqVerticalSpanElement.innerHTML;
	iqamaTimeDohrHorizontalSpanElement.innerHTML = iqamaTimeDohrVerticalSpanElement.innerHTML;
	iqamaTimeAsrHorizontalSpanElement.innerHTML = iqamaTimeAsrVerticalSpanElement.innerHTML;
	iqamaTimeMgrbHorizontalSpanElement.innerHTML = iqamaTimeMgrbVerticalSpanElement.innerHTML;
	iqamaTimeIshaHorizontalSpanElement.innerHTML = iqamaTimeIshaVerticalSpanElement.innerHTML;
	



if(counterForDohaTimeDisplay > 3) checkForTawkitDisplayUpdate();

}





dimmerBeforeValueElement.innerHTML = JS_DATA.ucJomoaDimmBefore;
dimmerAfterValueElement.innerHTML = JS_DATA.ucJomoaDimmAfter;




function decrementPrayerDurationFunction(prayerKey)
{

	switch(prayerKey)
	{

		case 'FAJR':
			JS_DATA.ucPrayDurationFAJR--;
			if(JS_DATA.ucPrayDurationFAJR < 5)
			{
				JS_DATA.ucPrayDurationFAJR = 5;
				return;
			}
			durationFajrValueElement.innerHTML = JS_DATA.ucPrayDurationFAJR;
			saveSettingsToStorageFunction();
			break;

		case 'DOHR':
			JS_DATA.ucPrayDurationDOHR--;
			if(JS_DATA.ucPrayDurationDOHR < 5)
			{
				JS_DATA.PrayDuration__DOHR = 5;
				return;
			}
			durationDohrValueElement.innerHTML = JS_DATA.ucPrayDurationDOHR;
			saveSettingsToStorageFunction();
			break;

		case 'ASR':
			JS_DATA.ucPrayDurationASSR--;
			if(JS_DATA.ucPrayDurationASSR < 5)
			{
				JS_DATA.ucPrayDurationASSR = 5;
				return;
			}
			durationAsrValueElement.innerHTML = JS_DATA.ucPrayDurationASSR;
			saveSettingsToStorageFunction();
			break;

		case 'MAGHRIB':
			JS_DATA.ucPrayDurationMGRB--;
			if(JS_DATA.ucPrayDurationMGRB < 5)
			{
				JS_DATA.ucPrayDurationMGRB = 5;
				return;
			}
			durationMgrbValueElement.innerHTML = JS_DATA.ucPrayDurationMGRB;
			saveSettingsToStorageFunction();
			break;

		case 'ISHA':
			JS_DATA.ucPrayDurationISHA--;
			if(JS_DATA.ucPrayDurationISHA < 5)
			{
				JS_DATA.ucPrayDurationISHA = 5;
				return;
			}
			durationIshaValueElement.innerHTML = JS_DATA.ucPrayDurationISHA;
			saveSettingsToStorageFunction();
			break;

		default:
			dolog('err_SSS');
			break;
	}

}




function incrementPrayerDurationFunction(prayerKey)
{

	switch(prayerKey)
	{

		case 'FAJR':
			JS_DATA.ucPrayDurationFAJR++;
			if(JS_DATA.ucPrayDurationFAJR > 40)
			{
				JS_DATA.ucPrayDurationFAJR = 40;
				return;
			}
			durationFajrValueElement.innerHTML = JS_DATA.ucPrayDurationFAJR;
			saveSettingsToStorageFunction();
			break;

		case 'DOHR':
			JS_DATA.ucPrayDurationDOHR++;
			if(JS_DATA.ucPrayDurationDOHR > 40)
			{
				JS_DATA.ucPrayDurationDOHR = 40;
				return;
			}
			durationDohrValueElement.innerHTML = JS_DATA.ucPrayDurationDOHR;
			saveSettingsToStorageFunction();
			break;

		case 'ASR':
			JS_DATA.ucPrayDurationASSR++;
			if(JS_DATA.ucPrayDurationASSR > 40)
			{
				JS_DATA.ucPrayDurationASSR = 40;
				return;
			}
			durationAsrValueElement.innerHTML = JS_DATA.ucPrayDurationASSR;
			saveSettingsToStorageFunction();
			break;

		case 'MAGHRIB':
			JS_DATA.ucPrayDurationMGRB++;
			if(JS_DATA.ucPrayDurationMGRB > 40)
			{
				JS_DATA.ucPrayDurationMGRB = 40;
				return;
			}
			durationMgrbValueElement.innerHTML = JS_DATA.ucPrayDurationMGRB;

			break;

		case 'ISHA':
			JS_DATA.ucPrayDurationISHA++;
			if(JS_DATA.ucPrayDurationISHA > 180)
			{
				JS_DATA.ucPrayDurationISHA = 180;
				return;
			}
			durationIshaValueElement.innerHTML = JS_DATA.ucPrayDurationISHA;
			saveSettingsToStorageFunction();
			break;

		default:
			dolog('err_eee');
			break;
	}
}






function extractBetween(str, aaa, bbb)
{
return str.substring(str.indexOf(aaa) + aaa.length, str.lastIndexOf(bbb));
}






function applyForcedVerticalClass()
{

	     if(JS_DATA.ucForcedVertical == 1) document.body.className = 'vrRIGHT';
	else if(JS_DATA.ucForcedVertical == 2) document.body.className = 'vrLEFT';
	
	if(JS_DATA.ucForcedVertical > 0) fontSizeUnit = "vh";

}


function restartApplicationReload()
{

let locationProtocol = window.location.protocol;
    
    switch(locationProtocol) {
        case 'http:':
        case 'https:':
            window.location.href = '/';
            break;
            
        case 'file:':
            window.location.href = 'index.html';
            break;
            
        case 'resource:':
            window.location.href = 'resource://android/assets/index.html';
            break;
            
        default:
            window.location.href = 'index.html';
            break;
    }


}


function ClearSETTINGS() 
{
localStorage.clear();
restartApplicationReload();
}


function checkForTawkitDisplayUpdate()
{

if(versionDisplayElement.innerHTML.indexOf(decodedHexAppName) < 0) isTawkitInfoMissing = true;
if(tawkitNameHorizontalElement.innerHTML.indexOf(decodedHexAppName) < 0) isTawkitInfoMissing = true;
if(cityCodeHorizontalElement.innerHTML.indexOf('&nbsp;') < 0) isTawkitInfoMissing = true;

if(isTawkitInfoMissing) { isSpecialTriggerActive = true; setTimeout('updateTimeAndPrayersFunction(true)', 700); }
}




function convertToArabicDigitsFunction(str)
{

if(!JS_DATA.ucIsArabicDigits) return str; 

let Lang_Digits_MAP = JS_DIGITS_MAPS["map_"+JS_DATA.ucLangNOW];
if(!Lang_Digits_MAP) Lang_Digits_MAP = JS_DIGITS_MAPS.map_AR;


	let newStr = "";
	let c = "";

	let iniStr = String(str);

	for(i = 0; i < iniStr.length; i++)
	{
		c = iniStr.charAt(i);
		if(c >= '0' && c <= '9') newStr += Lang_Digits_MAP[parseInt(c)]; else newStr += c;
	}

return newStr;
}




function initUILabels()
{

	azanPopupTitleVerticalElement.innerHTML = JS_eLang.cx_AzanCalling;
	azanPopupTitleHorizontalElement.innerHTML = JS_eLang.cx_AzanCalling;

	prayerNamesArray = JS_eLang.cx_NamesOfPrayings.split(",");
	weekDayNamesArray = JS_eLang.cx_NamesWeekDays.split(",");
	hijriMonthsArray = JS_eLang.cx_MenuMonthsHijri.split(",");

	prayerNameFajrVerticalSpanElement.innerHTML = prayerNamesArray[0];
	prayerNameShrqVerticalSpanElement.innerHTML = prayerNamesArray[1];
	prayerNameDohrVerticalSpanElement.innerHTML = prayerNamesArray[2];
	prayerNameAsrVerticalSpanElement.innerHTML = prayerNamesArray[3];
	prayerNameMgrbVerticalSpanElement.innerHTML = prayerNamesArray[4];
	prayerNameIshaVerticalSpanElement.innerHTML = prayerNamesArray[5];

	prayerNameFajrHorizontalSpanElement.innerHTML = prayerNameFajrVerticalSpanElement.innerHTML;
	prayerNameShrqHorizontalSpanElement.innerHTML = prayerNameShrqVerticalSpanElement.innerHTML;
	prayerNameDohrHorizontalSpanElement.innerHTML = prayerNameDohrVerticalSpanElement.innerHTML;
	prayerNameAsrHorizontalSpanElement.innerHTML = prayerNameAsrVerticalSpanElement.innerHTML;
	prayerNameMgrbHorizontalSpanElement.innerHTML = prayerNameMgrbVerticalSpanElement.innerHTML;
	prayerNameIshaHorizontalSpanElement.innerHTML = prayerNameIshaVerticalSpanElement.innerHTML;



	prayerNameFajrMiniHorizontalElement.innerHTML = prayerNameFajrVerticalSpanElement.innerHTML;
	prayerNameShrqMiniHorizontalElement.innerHTML = prayerNameShrqVerticalSpanElement.innerHTML;
	prayerNameDohrMiniHorizontalElement.innerHTML = prayerNameDohrVerticalSpanElement.innerHTML;
	prayerNameAsrMiniHorizontalElement.innerHTML = prayerNameAsrVerticalSpanElement.innerHTML;
	prayerNameMgrbMiniHorizontalElement.innerHTML = prayerNameMgrbVerticalSpanElement.innerHTML;
	prayerNameIshaMiniHorizontalElement.innerHTML = prayerNameIshaVerticalSpanElement.innerHTML;

	prayerNameFajrMiniVerticalElement.innerHTML = prayerNameFajrVerticalSpanElement.innerHTML;
	prayerNameShrqMiniVerticalElement.innerHTML = prayerNameShrqVerticalSpanElement.innerHTML;
	prayerNameDohrMiniVerticalElement.innerHTML = prayerNameDohrVerticalSpanElement.innerHTML;
	prayerNameAsrMiniVerticalElement.innerHTML = prayerNameAsrVerticalSpanElement.innerHTML;
	prayerNameMgrbMiniVerticalElement.innerHTML = prayerNameMgrbVerticalSpanElement.innerHTML;
	prayerNameIshaMiniVerticalElement.innerHTML = prayerNameIshaVerticalSpanElement.innerHTML;

	const forcedVerticalOptionHtml = "<span dir='ltr' >&nbsp; <span onclick='forcedVerticalModeParam(0);'>OFF</span> &nbsp;&nbsp; <span onclick='forcedVerticalModeParam(2);'>&#8630;</span> &nbsp; <span onclick='forcedVerticalModeParam(1);'>&#8631;</span>&nbsp;&nbsp;&nbsp;&nbsp;</span>";

	if(isTawkitApp) fullScreenButtonElement.style.display='none'; else fullScreenButtonElement.innerHTML = bulletPointSymbol + JS_eLang.cx_MenuFullScreen;
	
	document.getElementById('bottomMessagesButton').innerHTML 		= bulletPointSymbol + JS_eLang.cx_MenuMosqueMessage;
	document.getElementById('personalFilesButton').innerHTML 		= personalFilesMenuSymbol + JS_eLang.cx_PersoFILES;
	document.getElementById('themesButton').innerHTML 			= bulletPointSymbol + JS_eLang.cx_MenuThemes;
	document.getElementById('forcedVerticalOption').innerHTML 		= bulletPointSymbol + JS_eLang.cx_FORCED_VERTICAL + forcedVerticalOptionHtml;
	document.getElementById('primaryAzanSettingsTitle').innerHTML 		= bulletPointSymbol + JS_eLang.cx_MenuHijriClick + ' : <br>' + JS_eLang.cx_BOX_0;
	document.getElementById('alertLastMinuteLabel').innerHTML 	= JS_eLang.cx_MobileAlert;
	document.getElementById('optionsSettingsButton').innerHTML 		= optionsMenuSymbol + JS_eLang.cx_MenuOptions;
	document.getElementById('importExportButton').innerHTML 	= syncMenuSymbol + JS_eLang.cx_MenuSynchronize;
	document.getElementById('soundRemindersButton').innerHTML 	= syncMenuSymbol + JS_eLang.cx_SoundAlertsReminders;
	document.getElementById('azkarSettingsButton').innerHTML 			= azkarMenuSymbol + JS_eLang.cx_MenuAzkar;
	document.getElementById('slidesSettingsButton').innerHTML 			= bulletPointSymbol + JS_eLang.cx_MenuSliders;
	document.getElementById('adjustmentsButton').innerHTML 	= bulletPointSymbol + JS_eLang.cx_MenuAdjustments;
	document.getElementById('blackScreenSettingsButton').innerHTML 	= bulletPointSymbol + JS_eLang.cx_MenuBlackscreen;
	document.getElementById('locationSettingsButton').innerHTML 	= bulletPointSymbol + JS_eLang.cx_MenuLocation;
	document.getElementById('shortcutsInfoButton').innerHTML 		= bulletPointSymbol + JS_eLang.cx_ShortCUTS;
	document.getElementById('meteoApiButton').innerHTML 			= bulletPointSymbol + JS_eLang.cx_METEO_A;

	document.getElementById('dohrXminAsrLabel').innerHTML 		= JS_eLang.cx_DOHR_XMIN_ASR;
	document.getElementById('themesSectionTitle').innerHTML 		= JS_eLang.cx_MenuThemes;
	document.getElementById('primaryAzanLabel').innerHTML 		= JS_eLang.cx_PrimaryAzan.replace("|","<br>");
	document.getElementById('counterColorAlertLabel').innerHTML	= JS_eLang.cx_CounterColorAlert;
	document.getElementById('meteoActiveLabel').innerHTML 				= JS_eLang.cx_METEO;
	document.getElementById('meteoWithPrayersLabel').innerHTML 			= JS_eLang.cx_METEOPRYRS;
	document.getElementById('fontsSettingsTitle').innerHTML 			= JS_eLang.cx_APP_FONTS;
	document.getElementById('bigNextPrayCounterLabel').innerHTML 		= JS_eLang.cx_BiggerNextPray;


	document.getElementById('headerPrayerNameVertical').innerHTML 			= JS_eLang.cx_PRAYER;
	document.getElementById('headerAzanTimeVertical').innerHTML 				= JS_eLang.cx_AzanNAME;
	document.getElementById('headerIqamaTimeVertical').innerHTML 			= JS_eLang.cx_IQAMA;
	document.getElementById('shortcutsTitle').innerHTML 		= JS_eLang.cx_ShortCUTS;
	document.getElementById('shortcutsListContainer').innerHTML 		= JS_eLang.cx_ShortCutsTEXT;

	document.getElementById('bottomMessagesTitle').innerHTML 		= JS_eLang.cx_ADMINMSGS_0;
	document.getElementById('bottomMessagesInfoText').innerHTML	= JS_eLang.cx_ADMINMSGS_TEXTER;
	document.getElementById('messagesHelpText').innerHTML 		= JS_eLang.cx_ADMINMSGS_HELP;
	document.getElementById('addMessageButtonText').innerHTML 			= JS_eLang.cx_ADD_MSG;
	
	document.getElementById('userFilesInfoText').innerHTML 		= JS_eLang.cx_PersoFILESmessage+"<br>"+JS_eLang.cx_SlowDeviceAlert;
	document.getElementById('userFilesActiveLabel').innerHTML 	= JS_eLang.cx_EnablePersoFILES;

	document.getElementById('mosqueNameOptionLabel').innerHTML 		= JS_eLang.cx_MenuMosqueNAME;


	fastingMondayMessage 	= JS_eLang.cx_FastingMonday;
	fastingThursdayMessage 	= JS_eLang.cx_FastingThursday;

	document.getElementById('use24HoursLabel').innerHTML = JS_eLang.cx_OPTION_0;
	document.getElementById('fullClockLabel').innerHTML = JS_eLang.cx_OPTION_1;
	document.getElementById('dimPastPrayersLabel').innerHTML = JS_eLang.cx_OPTION_2;
	document.getElementById('showNightPrayersLabel').innerHTML = JS_eLang.cx_ShowNightPrayers;
	document.getElementById('middleSalatNamesLabel').innerHTML = JS_eLang.cx_MIDDLE_SALATS;
	document.getElementById('dateUpRightHrLabel').innerHTML = JS_eLang.cx_HRDATE_RIGHT;
	document.getElementById('useWcsvLabel').innerHTML = JS_eLang.cx_WCSV_MESSAGE;
	document.getElementById('verifyInternetLabel').innerHTML = JS_eLang.cx_VerifNET;
	document.getElementById('timesBgShadowsLabel').innerHTML = JS_eLang.cx_TimesBgShadows;
	document.getElementById('semiTransparentBgsLabel').innerHTML = JS_eLang.cx_SemiTransparent;
	document.getElementById('azanIqamaByVoiceLabel').innerHTML = JS_eLang.cx_ADJUST_2;
	document.getElementById('ramadanIshaLabel').innerHTML = JS_eLang.cx_ADJUST_3;
	document.getElementById('summerTimeLabel').innerHTML = JS_eLang.cx_SUMMERTIME;
	document.getElementById('forceOneHourMoreLabel').innerHTML = JS_eLang.cx_1MOREHOUR;
	document.getElementById('forceOneHourLessLabel').innerHTML = JS_eLang.cx_1LESSHOUR;
	document.getElementById('shortAzanLabel').innerHTML = JS_eLang.cx_eShortAZAN;
	document.getElementById('shortIqamaLabel').innerHTML = JS_eLang.cx_eShortIQAMA;
	document.getElementById('showAzanScreenLabel').innerHTML = JS_eLang.cx_ShowAzanScreen;
	document.getElementById('clockAdjustLabel').innerHTML = JS_eLang.cx_ClockADJUST;
	document.getElementById('ayatsLangLabel').innerHTML = JS_eLang.cx_SelectAyatsLang;
	document.getElementById('showIqamaScreenLabel').innerHTML = JS_eLang.cx_IQAMASCREEN;
	document.getElementById('iqamaHadithLabel').innerHTML = JS_eLang.cx_IqamaHadith;
	
	document.getElementById('themeTypeNoChangeLabel').innerHTML = JS_eLang.cx_Themes_NoChange;
	document.getElementById('themeTypeBySalatLabel').innerHTML = JS_eLang.cx_Themes_EverySalat;
	document.getElementById('themeTypeByDayLabel').innerHTML = JS_eLang.cx_Themes_EveryDay;
	document.getElementById('themeTypeRandomDailyLabel').innerHTML = JS_eLang.cx_Themes_RandomDaily;
	document.getElementById('eidFitrLabel').innerHTML = JS_eLang.cx_Eid_FitrTime;
	document.getElementById('eidAdhaLabel').innerHTML = JS_eLang.cx_Eid_AdhaTime;
	document.getElementById('slidesViewTimeLabel').innerHTML = bulletPointSymbol + JS_eLang.cx_SlidesViewTime;
	document.getElementById('tawkitViewTimeLabel').innerHTML = bulletPointSymbol + JS_eLang.cx_TawkitViewTime;
	


	prayerNameFajrAdjustElement.innerHTML = prayerNamesArray[0];
	prayerNameShrqAdjustElement.innerHTML = prayerNamesArray[1];
	prayerNameDohrAdjustElement.innerHTML = prayerNamesArray[2];
	prayerNameAsrAdjustElement.innerHTML = prayerNamesArray[3];
	prayerNameMgrbAdjustElement.innerHTML = prayerNamesArray[4];
	prayerNameIshaAdjustElement.innerHTML = prayerNamesArray[5];

	themeFajrCellElement.innerHTML = prayerNamesArray[0];
	themeShrqCellElement.innerHTML = prayerNamesArray[1];
	themeDohrCellElement.innerHTML = prayerNamesArray[2];
	themeAsrCellElement.innerHTML = prayerNamesArray[3];
	themeMgrbCellElement.innerHTML = prayerNamesArray[4];
	themeIshaCellElement.innerHTML = prayerNamesArray[5];

	
	themeSundayCellElement.innerHTML = weekDayNamesArray[0];
	themeMondayCellElement.innerHTML = weekDayNamesArray[1];
	themeTuesdayCellElement.innerHTML = weekDayNamesArray[2];
	themeWednesdayCellElement.innerHTML = weekDayNamesArray[3];
	themeThursdayCellElement.innerHTML = weekDayNamesArray[4];
	themeFridayCellElement.innerHTML = weekDayNamesArray[5];
	themeSaturdayCellElement.innerHTML = weekDayNamesArray[6];
	
	
	document.getElementById('iqamaFajrLabel').innerHTML = prayerNamesArray[0];
	document.getElementById('iqamaDohaLabel').innerHTML = JS_eLang.cx_DOHA;
	document.getElementById('iqamaDohrLabel').innerHTML = prayerNamesArray[2];
	document.getElementById('iqamaAsrLabel').innerHTML = prayerNamesArray[3];
	document.getElementById('iqamaMgrbLabel').innerHTML = prayerNamesArray[4];
	document.getElementById('iqamaIshaLabel').innerHTML = prayerNamesArray[5];

	document.getElementById('blackScreenInPrayingLabel').innerHTML = JS_eLang.cx_DIMMER_0;
	document.getElementById('dohaScreenSaverLabel').innerHTML = JS_eLang.cx_DIMMER_1;
	document.getElementById('ishaScreenSaverLabel').innerHTML = JS_eLang.cx_DIMMER_2;
	document.getElementById('jomoaLabel').innerHTML = weekDayNamesArray[5];
	document.getElementById('dimmerBeforeLabel').innerHTML = JS_eLang.cx_DIMMER_3;
	document.getElementById('dimmerAfterLabel').innerHTML = JS_eLang.cx_DIMMER_4;
	document.getElementById('prayerDurationsTitle').innerHTML = JS_eLang.cx_DIMMER_5;
	document.getElementById('blackScreenShowClockLabel').innerHTML = JS_eLang.cx_ClockInBlkScr;
	document.getElementById('blackScreenShowDateLabel').innerHTML = JS_eLang.cx_DateInBlkScr;

	document.getElementById('durationFajrLabel').innerHTML = prayerNamesArray[0];
	document.getElementById('durationDohrLabel').innerHTML = prayerNamesArray[2];
	document.getElementById('durationAsrLabel').innerHTML = prayerNamesArray[3];
	document.getElementById('durationMgrbLabel').innerHTML = prayerNamesArray[4];
	document.getElementById('durationIshaLabel').innerHTML = prayerNamesArray[5];

	document.getElementById('citySelectionTitle').innerHTML = JS_eLang.cx_TIMES_2 + " &nbsp; <span class='infoTextClass'>( if your city is not in list, <a target='_blank' href='https://www.tawkit.net/#contact'>contact author</a> )</span>";
	document.getElementById('importedTimesInfoTitle').innerHTML = JS_eLang.cx_TIMES_3;
	document.getElementById('importedTimesInfoText').innerHTML = JS_eLang.cx_TIMES_4;

	document.getElementById('themesInfoText').innerHTML = JS_eLang.cx_MISC_1;

	jomoaLabel = JS_eLang.cx_JOMOA;

	document.getElementById('jomoaSettingsTitle').innerHTML = bulletPointSymbol + JS_eLang.cx_JOMOA_ONOFF;
	document.getElementById('fullIqamaTimesLabel').innerHTML = JS_eLang.cx_IQAMAASHOUR;
	document.getElementById('iqamaCounterLabel').innerHTML = JS_eLang.cx_eCOUNTDOWNOnOff;
	document.getElementById('lastMinuteCounterLabel').innerHTML = JS_eLang.cx_1MIN_COUNTER;
	nextPrayerLabelHorizontalElement.innerHTML = JS_eLang.cx_NextPrayTime;
	jomoaLabelHorizontalElement.innerHTML = JS_eLang.cx_JOMOA;
	creditsContentElement.innerHTML = decodeHexStringFunction(encodedString5);
	
	
	document.getElementById('arabicDigitsLabel').innerHTML = JS_eLang.cx_UseArabicDIGITS;
	document.getElementById('marqueeActiveLabel').innerHTML = JS_eLang.cx_SCROLLABLE_MSG;
	document.getElementById('ramadanDaysLeftLabel').innerHTML = JS_eLang.cx_RamadanDaysLeftInfo;
	document.getElementById('hideIqamatLabel').innerHTML = JS_eLang.cx_HIDE_IQAMAT;
	document.getElementById('addZeroAmPmLabel').innerHTML = JS_eLang.cx_ZEROS_AMPM;
	document.getElementById('fiveBoxesOnlyLabel').innerHTML = JS_eLang.cx_5BOXES_ONLY;
	document.getElementById('iqamaPopupTextVertical').innerHTML = JS_eLang.cx_IQAMAT_SALAT;
	document.getElementById('iqamaPopupTextHorizontal').innerHTML = JS_eLang.cx_IQAMAT_SALAT;
	document.getElementById('dohaPopupTextVertical').innerHTML = JS_eLang.cx_DohaTime;
	document.getElementById('dohaPopupTextHorizontal').innerHTML = JS_eLang.cx_DohaTime;
	document.getElementById('iqamaAdjustmentsTitle').innerHTML = bulletPointSymbol + JS_eLang.cx_IQAMAT_SALAT;
	document.getElementById('azkarActiveLabel').innerHTML = JS_eLang.cx_AZKAR_0;
	document.getElementById('azkar5minPictureLabel').innerHTML = JS_eLang.cx_AZKAR_4;
	document.getElementById('azkarBgWhiteLabel').innerHTML = JS_eLang.cx_AzkarBGwhite;
	document.getElementById('slidesActiveLabel').innerHTML = JS_eLang.cx_SLIDERS_0;
	document.getElementById('slidesRandomLabel').innerHTML = JS_eLang.cx_Random_Slides;
	document.getElementById('jomoaOnHrLabel').innerHTML = JS_eLang.cx_JOMOA_SHOW;
	document.getElementById('jomoaAzanLabel').innerHTML = JS_eLang.cx_Jomoa_AzanOnOff;
	document.getElementById('middleVrNamesLabel').innerHTML = JS_eLang.cx_VR_MIDDLENAMES;
	document.getElementById('useImportedTimesLabel').innerHTML = JS_eLang.cx_UseImportedTimes;
	document.getElementById('repeatAzkarLabel').innerHTML = JS_RepeatAzkar;

	document.getElementById('azkarSabahLabel').innerHTML = JS_eLang.cx_AZKAR_SABAH;
	document.getElementById('azkarAsrLabel').innerHTML = JS_eLang.cx_AZKAR_ASR;
	document.getElementById('azkarMaghribLabel').innerHTML = JS_eLang.cx_AZKAR_MAGRIB;
	document.getElementById('azkarIshaLabel').innerHTML = JS_eLang.cx_AZKAR_ISHA;
	iqamaCounterLabelVerticalElement.innerHTML = JS_eLang.cx_MinutesToIqama;
	iqamaCounterLabelHorizontalElement.innerHTML = JS_eLang.cx_MinutesToIqama;
	document.getElementById('fullScreenCounterLabel').innerHTML = JS_eLang.cx_ADJUST_1;
	
	if(encodedString5.length != 500+62) setElementAttribute2Function();
	if(encodedString2.length != 100+10) setElementAttributeFunction();
	if(encodedString1.length != 60+12) setElementAttribute2Function();	
	
	

	if(isRTL)
	{
	rootElement.style.setProperty('--csvar_nowDIR', 'rtl');

			if(JS_DATA.ucLangNOW == "AR")
			{
			amSymbolHtml = "<span class='amPmArabicClass'>صباحا</span>";
			pmSymbolHtml = "<span class='amPmArabicClass'>مساء</span>";
			}
	}
	else
	{
	rootElement.style.setProperty('--csvar_nowDIR', 'ltr');
	}

}

function isRTLText(selectedLanguageCode)
{
	const arabicRegex = /[\u0600-\u06FF]/;
	return arabicRegex.test(selectedLanguageCode);
}




function isBetween(valueToTest, lowerBound, upperBound)
{
	return(valueToTest - lowerBound) * (valueToTest - upperBound) <= 0
}



function setGpsManuallyFunction()
{

	let gpsPositionString = JS_DATA.ucMeteoLatitude + ',' + JS_DATA.ucMeteoLongitude;
	
	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction("Enter Your GPS Position", gpsPositionString, 'setGpsManuallyFunction()', true);
	return;
	}

		let selectedLanguageCode = inputDialogValue.replace(' ', '');
		let gpsCoordinatesArray = selectedLanguageCode.split(',');
		JS_DATA.ucMeteoLatitude = gpsCoordinatesArray[0];
		JS_DATA.ucMeteoLongitude = gpsCoordinatesArray[1];

		saveSettingsToStorageFunction();
		displayGpsPosition();
		updateWeatherData();
}




function getFutureDateString(daysOffset)
{
	let tempDate = new Date();
	tempDate.setDate(tempDate.getDate() + daysOffset);

	let currentYear = tempDate.getFullYear();
	let currentMonth = ('0' + (tempDate.getMonth() + 1)).slice(-2);
	let formattedDay = ('0' + tempDate.getDate()).slice(-2);

	return currentYear + '-' + currentMonth + '-' + formattedDay;
}



function getCurrentTimestampForWeather()
{
	let weatherDate = new Date();
	if( (JS_DATA.ucLocalHoursAdjustment !== 0) && (allowedHourAdjustments.indexOf(JS_DATA.ucLocalHoursAdjustment) !== -1) )
	{weatherDate.setTime(weatherDate.getTime() + (JS_DATA.ucLocalHoursAdjustment*60*60*1000));}
	
	let currentYear = weatherDate.getFullYear();
	let hexString = ('0' + weatherDate.getHours()).slice(-2);
	let timestampString = getFutureDateString(0);
	return timestampString + 'T' + hexString + ':00';
}






function getGpsPositionFunction()
{
	if(navigator.geolocation)
	{
		gpsPositionDisplayElement.innerHTML = '...';
		navigator.geolocation.getCurrentPosition(onGpsSuccess);
	}

}




function onGpsSuccess(position)
{
	JS_DATA.ucMeteoLatitude = position.coords.latitude;
	JS_DATA.ucMeteoLongitude = position.coords.longitude;
	saveSettingsToStorageFunction();
	displayGpsPosition();
	updateWeatherData();
}




function highlightCurrentLanguage()
{
document.getElementById(currentLanguageCode).style.color = 'white';
document.getElementById('e7'+JS_DATA.ucAyatsLANG).style.color = 'white';
if(!JS_DATA.ucVrMiniTitlesVisible==1) document.getElementById('headerRowVertical').style.display = 'none';
}




function displayGpsPosition()
{
	gpsPositionDisplayElement.innerHTML = JS_DATA.ucMeteoLatitude + ' , ' + JS_DATA.ucMeteoLongitude;
}



function changeThemeRandomly()
{
	let themeIndexFromSettings = parseInt(randomThemesList[Math.floor(Math.random()*randomThemesList.length)]);
	if(themeIndexFromSettings !== currentThemeIndex)
	{
	currentThemeIndex = themeIndexFromSettings; 
	setThemeFunction(currentThemeIndex, true);
	}
}


function changeThemeByDay()
{
	let currentDateObj = new Date();
	let dayIndex = currentDateObj.getDay();
	
	let dailyThemeNumber = parseInt(dailyThemesArray[dayIndex]);
	if(!Number.isInteger(dailyThemeNumber)) return;
	
	let themeIndexFromSettings = dailyThemeNumber;
	if(themeIndexFromSettings !== currentThemeIndex)
	{
	currentThemeIndex = themeIndexFromSettings; 
	setThemeFunction(currentThemeIndex, true);
	}
}




function applyThemeChange()
{

	switch(JS_DATA.ucThemesActiveType)
	{

		case 0	: 
				break;

		case 1	: 
				changeThemeByPrayer(currentTimeInMinutes);
				break;

		case 2	: 
				changeThemeByDay();
				break;

		case 3	: 
				changeThemeRandomly();
				break;

		default:
			dolog('err_thm_type');
			break;
	}
}



function updateWeatherData()
{
	if(JS_DATA.ucIsMeteoOn == 0) return;
	if(JS_DATA.ucMeteoLatitude == 0)
	{
		weatherTempVerticalElement.innerHTML = "-";
		weatherTempHorizontalElement.innerHTML = "-";
		return;
	}

	fetch('https://api.open-meteo.com/v1/forecast?latitude=' + JS_DATA.ucMeteoLatitude + '&longitude=' + JS_DATA.ucMeteoLongitude + '&hourly=temperature_2m,weathercode&forecast_days=7&timezone=auto')
		.then(text => text.json())
		.then(data =>
		{
			weatherApiData = data;
			refreshWeatherDisplay();
		})
		.catch(error =>
		{
			console.log('TWKT : '+error);
		})
}




function getWeatherIcon(weatherCode, timeMinutes)
{
	let iconFileName = '0.png';

	if(weatherCode == 0)
	{
		if((timeMinutes >= maghribTimeInMinutes) || (timeMinutes < fajrTimeInMinutes))
			 iconFileName = 'i_clearNight.png';
		else iconFileName = 'i_clearDay.png';
	}
	else if(isBetween(weatherCode,  1,  3)) iconFileName = 'i_clouds.png';
	else if(isBetween(weatherCode,  4,  9)) iconFileName = 'i_smoke.png';
	else if(isBetween(weatherCode, 10, 19)) iconFileName = 'i_fogmist.png';
	else if(isBetween(weatherCode, 20, 29)) iconFileName = 'i_snowgrains.png';
	else if(isBetween(weatherCode, 30, 39)) iconFileName = 'i_sandstorm.png';
	else if(isBetween(weatherCode, 40, 49)) iconFileName = 'i_fogmist.png';
	else if(isBetween(weatherCode, 50, 69)) iconFileName = 'i_dizzle.png';
	else if(isBetween(weatherCode, 70, 79)) iconFileName = 'i_snow.png';
	else if(isBetween(weatherCode, 80, 99)) iconFileName = 'i_rainhard.png';

	return iconFileName;
}





function refreshWeatherDisplay()
{

	if(JS_DATA.ucIsMeteoOn == 0) return;

	if(!weatherApiData.timezone) return;

	let currentHourTimestamp = getCurrentTimestampForWeather();
	let dateIndex = findIndexInArray(currentHourTimestamp, weatherApiData.hourly.time);

	if(dateIndex > -1)
	{
		let temperaturesArray = weatherApiData.hourly.temperature_2m.slice(0, 24);
		temperatureUnit = weatherApiData.hourly_units.temperature_2m;
		temperatureUnit = temperatureUnit.replace('°', '');
		currentTemperature = Math.floor(weatherApiData.hourly.temperature_2m[dateIndex]);
		weatherTempVerticalElement.innerHTML = convertToArabicDigitsFunction(currentTemperature) + '°';
		weatherTempHorizontalElement.innerHTML = weatherTempVerticalElement.innerHTML;
		let weatherCodeValue = parseInt(weatherApiData.hourly.weathercode[dateIndex]);
		weatherIconHorizontalElement.title = weatherCodeValue;
		weatherIconVerticalElement.title = weatherCodeValue;
		let weatherIconName = getWeatherIcon(weatherCodeValue, currentTimeInMinutes);
		weatherIconHorizontalElement.src = 'images/meteo/' + weatherIconName;
		weatherIconVerticalElement.src = weatherIconHorizontalElement.src;
		let maxTemp = Math.floor(Math.max.apply(null, temperaturesArray));
		let minTemp = Math.floor(Math.min.apply(null, temperaturesArray));
		weatherMinMaxHorizontalElement.innerHTML = convertToArabicDigitsFunction(maxTemp + '° ' + minTemp + '°');
		weatherMinMaxVerticalElement.innerHTML = convertToArabicDigitsFunction(maxTemp + '° ' + minTemp + '°');
	}
	else
	{
		weatherTempVerticalElement.innerHTML = '-';
		weatherTempHorizontalElement.innerHTML = '-';
		weatherIconHorizontalElement.src = 'images/meteo/0.png';
		weatherMinMaxHorizontalElement.innerHTML = '-';
		weatherMinMaxVerticalElement.innerHTML = '-';
	}


	if(JS_DATA.ucMeteoWithPrayers == 1)
	{
		for(let i = 0; i < 6; i++)
		{
			prayerTimestamp = getFutureDateString(0) + prayerHourPrefixes[i];
			dateIndex = findIndexInArray(prayerTimestamp, weatherApiData.hourly.time);
			let weatherHorizontalElement = document.getElementById('HmtALT' + i);
			let weatherVerticalElement = document.getElementById('VmtALT' + i);

			if(dateIndex > -1)
			{
				currentTemperature = Math.floor(weatherApiData.hourly.temperature_2m[dateIndex]);
				let prayerWeatherCode = parseInt(weatherApiData.hourly.weathercode[dateIndex]);

				let prayerWeatherIcon = getWeatherIcon(prayerWeatherCode, prayerTimesMinutesArray[i]);
				weatherHorizontalElement.title = prayerWeatherCode;
				weatherHorizontalElement.style.background = "url(./images/meteo/" + prayerWeatherIcon + ") no-repeat left bottom";
				weatherHorizontalElement.style.backgroundSize = "70% auto";
				weatherHorizontalElement.innerHTML = convertToArabicDigitsFunction(currentTemperature) + "°";

				weatherVerticalElement.title = prayerWeatherCode;
				weatherVerticalElement.style.background = weatherHorizontalElement.style.background;
				weatherVerticalElement.style.backgroundSize = "70% auto";
				weatherVerticalElement.innerHTML = weatherHorizontalElement.innerHTML;
				
			
			}
			else
			{
				weatherHorizontalElement.innerHTML = "*";
				weatherVerticalElement.innerHTML = "*";
			}

		}
	}

}






function checkAndRestoreIqamaCounter()
{
let remainingIqamaMinutes = 0;
setTimeout('updateTimeAndPrayersFunction(true)', unusedNumberString2);

	switch(true)
	{
		case ( (currentTimeInMinutes > fajrTimeInMinutes) && (currentTimeInMinutes < (fajrTimeInMinutes + JS_DATA.ucIqamaFAJR)) )	:
							remainingIqamaMinutes = (fajrTimeInMinutes + JS_DATA.ucIqamaFAJR) - currentTimeInMinutes;
							startIqamaCounterFunction(remainingIqamaMinutes, JS_DATA.ucPrayDurationFAJR, JS_DATA.ucIqamaFAJR); 
							positionHighlightBar(prayerRowFajrVerticalElement, prayerCellFajrHorizontalElement);
							showIqamaCounter(); break;
		
		
		case ( (currentTimeInMinutes > shuruqTimeInMinutes) && (currentTimeInMinutes < (shuruqTimeInMinutes + JS_DATA.ucIqamaSHRQ)) )	:	
							remainingIqamaMinutes = (shuruqTimeInMinutes + JS_DATA.ucIqamaSHRQ) - currentTimeInMinutes;
							startIqamaCounterFunction(remainingIqamaMinutes, 0, JS_DATA.ucIqamaSHRQ); 
							positionHighlightBar(prayerRowShrqVerticalElement, prayerCellShrqHorizontalElement);
							showIqamaCounter(); break;
		
		
		case ( (currentTimeInMinutes > dohrTimeInMinutes) && (currentTimeInMinutes < (dohrTimeInMinutes + JS_DATA.ucIqamaDOHR)) )	:	
							remainingIqamaMinutes = (dohrTimeInMinutes + JS_DATA.ucIqamaDOHR) - currentTimeInMinutes;
							startIqamaCounterFunction(remainingIqamaMinutes, JS_DATA.ucIqamaDOHR, JS_DATA.ucIqamaDOHR); 
							positionHighlightBar(prayerRowDohrVerticalElement, prayerCellDohrHorizontalElement);
							showIqamaCounter(); break;
		
		
		case ( (currentTimeInMinutes > asrTimeInMinutes) && (currentTimeInMinutes < (asrTimeInMinutes + JS_DATA.ucIqamaASSR)) )	:	
							remainingIqamaMinutes = (asrTimeInMinutes + JS_DATA.ucIqamaASSR) - currentTimeInMinutes;
							startIqamaCounterFunction(remainingIqamaMinutes, JS_DATA.ucIqamaASSR, JS_DATA.ucIqamaASSR); 
							positionHighlightBar(prayerRowAsrVerticalElement, prayerCellAsrHorizontalElement);
							showIqamaCounter(); break;
		
		
		case ( (currentTimeInMinutes > maghribTimeInMinutes) && (currentTimeInMinutes < (maghribTimeInMinutes + JS_DATA.ucIqamaMGRB)) )	:	
							remainingIqamaMinutes = (maghribTimeInMinutes + JS_DATA.ucIqamaMGRB) - currentTimeInMinutes;
							startIqamaCounterFunction(remainingIqamaMinutes, JS_DATA.ucIqamaMGRB, JS_DATA.ucIqamaMGRB); 
							positionHighlightBar(prayerRowMgrbVerticalElement, prayerCellMgrbHorizontalElement);
							showIqamaCounter(); break;
		
		
		case ( (currentTimeInMinutes > ishaTimeInMinutes) && (currentTimeInMinutes < (ishaTimeInMinutes + JS_DATA.ucIqamaISHA)) )	:	
							remainingIqamaMinutes = (ishaTimeInMinutes + JS_DATA.ucIqamaISHA) - currentTimeInMinutes;
							startIqamaCounterFunction(remainingIqamaMinutes, JS_DATA.ucPrayDurationISHA, JS_DATA.ucIqamaISHA); 
							positionHighlightBar(prayerRowIshaVerticalElement, prayerCellIshaHorizontalElement);
							showIqamaCounter(); break;
	}




}

function initializeThemeScheduler()
{
isThemeChangeBySalatActive = true;
applyThemeChange();
}



//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
function exportSettingsFunction()
{
const exportStatusDiv = document.getElementById('exportStatusDiv');
exportStatusDiv.innerHTML = "connecting ...";

let exportDataLines = [];
exportDataLines.push(' Tawkit.net | برنامج التوقيت\r\n\r\n ملف الإعدادات العامة لتطبيق التوقيت ');
exportDataLines.push('\r\n الإعدادات , الرسائل , الشرائح و بيانات أوقات الصلاة السنوية ');
exportDataLines.push('\r\n\r\n Tawkit.net Application Global Settings File ');
exportDataLines.push('\r\n SETTINGS, MESSAGES, SLIDES & YEARLY PRAYER TIMES DATA ');
exportDataLines.push('\r\n-------------------------------------------------------------------');
exportDataLines.push('\r\n After modifications, you can upload your file here : https://settings.tawkit.net ');
exportDataLines.push('\r\n The website will give you an Import-ID to use in Tawkit app');
exportDataLines.push('\r\n-------------------------------------------------------------------');
exportDataLines.push('\r\n\r\n Documentation :');
exportDataLines.push('\r\n - Change ONLY text between " "');
exportDataLines.push('\r\n - SETTINGS Explanation here https://settings.tawkit.net/help.html\r\n - Do NOT use the character " in your text ');
exportDataLines.push('\r\n - If app is blocked after a bad file import,\r\n   click 7 times on weather-forecasts place to reset app');


exportDataLines.push('\r\n\r\n\r\n-------------------------------- SETTINGS - إعدادات التطبيق -----------------');
for(let arrayIndex in JS_DATA) 
{
let settingValue = JS_DATA[arrayIndex];
exportDataLines.push('\r\n'+arrayIndex+'\t:\t"'+settingValue+'"');
}

exportDataLines.push('\r\n\r\n-------------------------------- MESSAGES BOTTOM SCREEN - إعلانات أسفل الشاشة -----');
for(let i = 0; i < JS_BOTTOM_MSGS.length; i++)
{
exportDataLines.push('\r\n'+'MsgBTM_\t:\t"'+JS_BOTTOM_MSGS[i]+'"');
}

exportDataLines.push('\r\n\r\n-------------------------------- SLIDES - الشرائح --------------------------------');
for(let i = 0; i < JS_SLIDES_DATA.length; i++)
{
exportDataLines.push('\r\n'+'Slide_\t:\t"'+JS_SLIDES_DATA[i]+'"');
}

exportDataLines.push('\r\n\r\n-------------------------------- YEARLY PRAYER TIMES - أوقات الصلاة السنوية -------');
exportDataLines.push('\r\nYou can use this link to create new prayer times : https://www.tawkit.net/wtimes/ \r\nALL TIMES 24 HOURS ');
exportDataLines.push('\r\n\r\n----------------------------------------------');
for(let i = 0; i < JS_TIMES.length; i++)
{
exportDataLines.push('\r\n"'+JS_TIMES[i]+'"');
}
exportDataLines.push('\r\n\r\n\r\n--------------------------------------------------------------------- END. ');



fetch('https://settings.tawkit.net/', {method:'POST', headers:{'Content-Type':'application/json',}, body: exportDataLines,})
.then(rez => rez.text())
.then(timeParts =>	{
					if((timeParts.indexOf('{[(') !== -1) && (timeParts.indexOf(')]}') !== -1)  && (timeParts.length == 14) )
					{
					let exportId = extractBetween(timeParts,'{[(',')]}');
					let exportUrl = "https://settings.tawkit.net/?dw="+exportId;
					exportStatusDiv.innerHTML = "<a target='_blank' href='"+exportUrl+"'>"+exportUrl+"</a><br>"+JS_eLang.cx_DLsettingsFile;
					}
					else
					{exportStatusDiv.innerHTML = "export_error";}
				})
.catch(error => {exportStatusDiv.innerHTML = "connection_error"; dolog(error);})

}



function parseImportLine(rawLine) 
{
let parsedLineParts;
parsedLineParts	= rawLine.replace(":","@^°"); 
parsedLineParts	= parsedLineParts.split("@^°");

parsedLineParts[0]  = parsedLineParts[0].replace(/\t/g, "");
parsedLineParts[1]  = parsedLineParts[1].replace(/\t/g, "");

parsedLineParts[0].trim();
parsedLineParts[1].trim();

parsedLineParts[1]  = extractBetween(parsedLineParts[1],'"','",');

return parsedLineParts;
}


function importByIdFunction() 
{
const importByIdStatusDiv = document.getElementById('importByIdStatusDiv');

	if(!isInputDialogOpen)
	{
	inputDialogValue	= "";
	isInputDialogOpen	= false;
	openInputDialogFunction(JS_eLang.cx_EnterImportID, '', 'importByIdFunction()', true);
	return;
	}
	
	
	let importId = inputDialogValue;
	
	if( (importId.length == 9)&&(importId.indexOf('-')==4) )
	{
		importByIdStatusDiv.innerHTML = "Connection ...";
		fetch('https://settings.tawkit.net/?importID='+importId)
		.then(importResponse => importResponse.text())
		.then(importFileContent =>
		{
		
			if(  (importFileContent.indexOf(iqamaFajrKey)	!== -1) || 
			     (importFileContent.indexOf(bottomMessageKeyPrefix)		!== -1) || 
			     (importFileContent.indexOf(slideKeyPrefix)		!== -1) || 
			     (importFileContent.indexOf(yearEndDatePattern)		!== -1)
			   ) 
	    		{
	    		processImportData(importFileContent,importByIdStatusDiv);
	    		}
	    		else{
	    			let importErrorMessage = "Invalid_Import_File";
	    			if(importFileContent.indexOf("s404") !== -1) importErrorMessage = "Import_File_Not_Available.";
	    			importByIdStatusDiv.innerHTML = importErrorMessage;
	    			}
		})
		.catch(error =>
		{
		console.log('TWKT : '+error);
		importByIdStatusDiv.innerHTML = "errorImport";
		})
	}
	else importByIdStatusDiv.innerHTML = "INVALID ID !";
	

	
}

function processImportData(importData, statusDiv) 
{

if(importData.length > 90000)
{
statusDiv.innerHTML = "ERROR: Import File Too Big. File must be less than 90 kb";
return;
}

let hasSettingsSection 		= (importData.indexOf(iqamaFajrKey) !== -1);
let hasMessagesSection 	= (importData.indexOf(bottomMessageKeyPrefix) !== -1);
let hasSlidesSection		 	= (importData.indexOf(slideKeyPrefix) !== -1);
let hasTimesSection			= (importData.indexOf(yearEndDatePattern) !== -1);

let importLines = importData.split('\r\n');
let importKey, importValue, importKeyValue;



if(hasMessagesSection)	JS_BOTTOM_MSGS = [];
if(hasSlidesSection)			JS_SLIDES_DATA = [];
if(hasTimesSection)			importedTimesArray = [];

		for (let i = 0; i < importLines.length; i++) 
		{
		let trimmedLine = importLines[i].trim();
		
				
				if(hasSettingsSection)
				{
					if( (trimmedLine.indexOf('uc') == 0)&&(trimmedLine.indexOf(':')>0)&&(trimmedLine.indexOf('",')>0) )
					{
					importKeyValue  = parseImportLine(trimmedLine);
					importKey  = importKeyValue[0];
					importValue  = importKeyValue[1];
					
						if(importKey in JS_DATA)
						{
						if(Number.isInteger(JS_DATA[importKey])) JS_DATA[importKey] = parseInt(importValue); else JS_DATA[importKey] = importValue;
							
						}
					}
				}
				
				
				
				if(hasMessagesSection)
				{
					if( (trimmedLine.indexOf('MsgBTM_') == 0)&&(trimmedLine.indexOf(':')>0)&&(trimmedLine.indexOf('|')>0)&&(trimmedLine.indexOf('",')>0) )
					{
					importKeyValue  = parseImportLine(trimmedLine);
					importValue  = importKeyValue[1];
					JS_BOTTOM_MSGS.push(importValue);
					}	
				}
	

				if(hasSlidesSection)
				{
					if( (trimmedLine.indexOf('Slide_') == 0)&&(trimmedLine.indexOf(':')>0)&&(trimmedLine.indexOf('",')>0) )
					{
					importKeyValue  = parseImportLine(trimmedLine);
					importValue  = importKeyValue[1];
					
					importValue = sanitizeInputString(importValue);
					
					JS_SLIDES_DATA.push(importValue);
					}	
				}

				if(hasTimesSection)
				{
					if( (trimmedLine.indexOf('~~~~~') == 6)&&(trimmedLine.indexOf('"')==0)&&(trimmedLine.indexOf(',')==47) )
					{
					importKey = extractBetween(trimmedLine,'"','",');
					importedTimesArray.push(importKey);
					}	
				}

	


		}




if(hasTimesSection) JS_DATA.ucUseImportedTimes = 1;

if(hasSettingsSection)		saveSettingsToStorageFunction();
if(hasMessagesSection)	saveBottomMessages();
if(hasSlidesSection)			saveSlidesToStorage();
if(hasTimesSection)			localStorage.setItem('STORAGE_USER_IMPORTED_TIMES', importedTimesArray);



statusDiv.innerHTML = "Import COMPLETE. Restarting ...";
setTimeout('restartApplicationReload()', 1800);
}



function importLocalFileFunction() 
{
const importLocalStatusDiv = document.getElementById('importLocalStatusDiv');

let selectedFile = document.getElementById('importFileInput').files[0];
if(!selectedFile) {importLocalStatusDiv.innerHTML = "Please browser the file to import."; return;}

let fileReader = new FileReader();

  fileReader.onload = function(eve) 
				  {
					let fileContent = fileReader.result;
						if(  (fileContent.indexOf(iqamaFajrKey)	!== -1) || 
						     (fileContent.indexOf(bottomMessageKeyPrefix)		!== -1) || 
						     (fileContent.indexOf(slideKeyPrefix)		!== -1) || 
						     (fileContent.indexOf(yearEndDatePattern)		!== -1)
						   ) 
				    		{
				    		processImportData(fileContent,importLocalStatusDiv);
				    		}
				    	else {importLocalStatusDiv.innerHTML = "Invalid_Settings_File";}
				  };

fileReader.readAsText(selectedFile);
}



rootElement.onkeydown = function(e) 
{
let keyCode = e.keyCode;



		const arrowKeysArray = [37, 38, 39, 40, 13];

		if(arrowKeysArray.indexOf(keyCode) !== -1)
		{
           e.preventDefault();
			
			
			if(!isPointerModeActive)
			{
			tvPointerElement.style.visibility = 'visible';
			isPointerModeActive = true;
			return;
			}
			else
			{
					pointerLeft = tvPointerElement.offsetLeft;
					pointerTop = tvPointerElement.offsetTop;
					
					
	            	
					showPointer();
					
					switch(keyCode)
					{
					case 37	:	pointerLeft = pointerLeft - tvPointerStepPixels;
								if(pointerLeft < - 10) pointerLeft = screenWidth -10;
								tvPointerElement.style.left 	= pointerLeft + 'px';	break;
					
					case 39	: 	pointerLeft = pointerLeft + tvPointerStepPixels;
								if(pointerLeft > (screenWidth -15)) pointerLeft = -10;
								tvPointerElement.style.left 	= pointerLeft + 'px';	break;
								
					case 38	: 	pointerTop = pointerTop - tvPointerStepPixels;
								if(pointerTop < - 10) pointerTop = screenHeight -10;
								tvPointerElement.style.top 	= pointerTop + 'px';	break;
								
					case 40	:	pointerTop = pointerTop + tvPointerStepPixels;
								if(pointerTop > (screenHeight -15)) pointerTop = -10;
								tvPointerElement.style.top 	= pointerTop + 'px';	break;
						

							case 13 :
							{
							    
							    let clickX = pointerLeft + Math.floor(tvPointerElement.offsetWidth  / 2);
							    let clickY = pointerTop + Math.floor(tvPointerElement.offsetHeight / 2);
							
							    let xView = clickX;
							    let yView = clickY;
							
							    if(JS_DATA.ucForcedVertical == 1)
							    {
							        const m = transformCoordsVrRightFunction(clickX, clickY);
							        xView = m.xView;
							        yView = m.yView;
							    }
							    else if(JS_DATA.ucForcedVertical == 2)
							    {
							        const m = transformCoordsVrLeftFunction(clickX, clickY);
							        xView = m.xView;
							        yView = m.yView;
							    }
							
							    let elementAtPoint = document.elementFromPoint(xView, yView);
							    if (elementAtPoint)
							    {
							        simulateClick(xView, yView);
							    }
							}
							break;


					}
			
			return;
			}
		}

}


function simulateClick(simulatedX,simulatedY) 
{


    hideElementByIdFunction('tvPointerImage');
    let targetElement = document.elementFromPoint(simulatedX, simulatedY); 
    showElementFunction('tvPointerImage');

    if (!targetElement) return;

	
	 if(targetElement)
		{
		const clickTimestamp = Date.now();
			
			  if ((clickTimestamp - lastTvClickTimestamp) < 290)
			  {
				targetElement.ondblclick?.();
			  }
			else
			  {
			   targetElement.click();
			  }
		
		lastTvClickTimestamp = clickTimestamp;
		}
	
}



function showPointer() 
{
	tvPointerElement.style.visibility = 'visible';
	isPointerModeActive = true;
	clearTimeout(pointerTimeout);
	pointerTimeout = setTimeout(() => {isPointerModeActive = false; tvPointerElement.style.visibility='hidden';}, 5000);
}






function toggleAutoStartFunction()
{
if(!isTawkitApp) return;

	if(autoStartCheckboxElement.checked )
	{
	window.location = "tawkit_enable_home";
	autoStartLabelElement.style.color = 'orange';
	}
	else{
		 window.location = "tawkit_disable_home";
		}
}




function checkAutoStartStatus()
{
	if(isTawkitApp) 
	{
	showElementBlockFunction('autoStartContainer'); 
	window.location = "tawkit_check_home";
	} 
}

window.onHomeAppStatus = function(isAutoStartEnabled)
{
	if(isAutoStartEnabled) 
	{autoStartCheckboxElement.checked = true;	autoStartLabelElement.style.color = 'green';} 
	else
	{autoStartCheckboxElement.checked = false; autoStartLabelElement.style.color = 'gray';} 
};

window.onHomeAppDisabled = function(isAutoStartDisabled) 
{
		if (isAutoStartDisabled) 
		{
		autoStartCheckboxElement.checked =  false;
		autoStartLabelElement.style.color = 'gray';
		doAlert("AUTO-START DISABLED");
		} 
    else {doAlert("Failed to disable Auto-Start");}
};






initDecodeStringsFunction();  

addAboutMenuItemFunction();




initUILabels();




updateUserFilesSectionFunction(1);


applyForcedVerticalClass();

applyArabicDigits(JS_DATA.ucIsArabicDigits,false);

updateWcsvModeFunction((JS_DATA.ucWcsvIsActive == 1),false);
updateMarqueeCheckboxFunction();
updateDohaScreenSaverCheckboxFunction();
updateIshaScreenSaverCheckboxFunction();
updateHomeModeLayoutFunction();
applyHrLayoutFunction();
updateAddZeroAmPmCheckboxFunction();
updateVrNamesPositionFunction();


updateAthanIqamaDisplayFunction();

updateFixedTimesDisplayFunction();
showInitialMessagesFunction();
updateMosqueAndDateDisplayFunction();

updateVoiceCheckboxDependentsFunction();
document.title = jcTawkitVersionSTR;
updateAzkarOnSettingsFunction();


setTimeout(populateCountryListFunction, 2500);


detectOrientationFunction();
displayGpsPosition();

updateMeteoVisibilityFunction(false);
updateMeteoWithPrayersVisibilityFunction(false);
updateDohrXminAsrDisplay();
updatePrimaryAzanDisplay();
displayBottomMessagesTable();
updateBottomMessages();
displaySlidesTable();
displayRemindersTable();

updateAzkar5minCheckboxes();
buildAzkarArraysFunction();
duplicateMainAzkar(); 
updateLastMinuteCounterCheckboxFunction();
updateFullScreenCounterCheckboxFunction();
updateJomoaAzanCheckboxFunction();
updateShowIqamaScreenCheckboxFunction();
updateShowAzanScreenCheckboxFunction();
highlightCurrentLanguage();
updateDimPastPrayersCheckboxFunction();
updateNightPrayersVisibilityFunction();
updateHrNamesPositionFunction(true);
updateDatePositionHrFunction();
updatePsFlagVisibilityFunction();
updateVerifyInternetCheckboxFunction();
setFontRadioButtonsFunction();
updateTimesBgShadowsFunction();
updateSemiTransparentBgsFunction();
updateCounterColorAlertCheckboxFunction();
updateIqamaHadithCheckboxFunction();
updateAlertLastMinuteCheckboxFunction();
updateBlackScreenClockCheckboxFunction();
updateBlackScreenDateCheckboxFunction();
updateClockDisplayFunction(JS_DATA.ucClockIsFull == 1);
saveThemesForEachSalat();
saveThemesForEachDay();
displayRandomThemesList();

setThemeTypeRadioButton();
adjustAzkarFontSize();
setTimeout(initializeThemeScheduler, 3000);
updateEidFitrCheckboxFunction();
updateEidAdhaCheckboxFunction();
updateSlidesViewTimeDisplay();

updateTawkitViewTimeDisplay();
if(importedTimesInvalid) doAlert("Invalid Imported Times ! Settings File Used.");
updateUseImportedTimesCheckbox();
updateClockAdjustDisplay();
DisableSelection(document.body);

window.addEventListener('load', async () => {
    dolog('_LOAD_COMPLETED_');
    await sleepFunction(50);
	setTimeout(startClockIntervalFunction, 1000);
	setTimeout(checkAndRestoreIqamaCounter, 1000);
	calculateAndDisplayTimesFunction();
	updateTimeAndPrayersFunction(false);
	window.onfocus = function(){calculateAndDisplayTimesFunction(); updateTimeAndPrayersFunction(true);}
	setTimeout(checkAutoStartStatus, 3000);
	window.addEventListener('resize', handleResizeFunction);
	handleResizeFunction();
	await sleepFunction(2500);
	updateWeatherData();
});

