**************************************************************
 _____              ____             _     _                 *
|  ___| __ ___  ___|  _ \ __ _ _ __ (_) __| |                *
| |_ | '__/ _ \/ _ \ |_) / _` | '_ \| |/ _` |                *
|  _|| | |  __/  __/  _ < (_| | |_) | | (_| |                *
|_|  |_|  \___|\___|_| \_\__,_| .__/|_|\__,_|                *
 ____                      _  |_|            _               *
|  _ \  _____      ___ __ | | ___   __ _  __| | ___ _ __     *
| | | |/ _ \ \ /\ / / '_ \| |/ _ \ / _` |/ _` |/ _ \ '__|    *
| |_| | (_) \ V  V /| | | | | (_) | (_| | (_| |  __/ |       *
|____/ \___/ \_/\_/ |_| |_|_|\___/ \__,_|\__,_|\___|_|       *
*                                                            *
*                                                            *
*     FreeRapid Downloader                                   *
*      - Ladislav Vitasek alias Vity (c) 2009                *
*Dom.str�nka/Forum/Bugtracker: http://wordrider.net/freerapid*
*E-Mail: info@wordrider.net - n�vrhy                         *
*Posledn� zm�na: 2009-04-14                                  *
**************************************************************

=======================================
Obsah:
   I.   Co je FreeRapid Downloader
  II.   Syst�mov� po�adavky
 III.   Jak spustit FreeRapid
  IV.   Zn�m� probl�my a omezen�
   V.   �e�en� probl�m�
  VI.   Hl�en� chyb
 VII.   Jak podpo�it FreeRapid
VIII.   �asto kladen� ot�zky a odpov�di
=======================================


I.    Co je FreeRapid Downloader
=======================================

FreeRapid downloader je jednoduch� aplikace napsan� v jazyku Java, kter� umo��uje pohodln� stahov�n� soubor� z datov�ho ulo�i�t� Rapidshare a mnoha dal��ch slu�eb.

Hlavn� vlastnosti:
 - stahov�n� soubor� z v�ce slu�eb najednou
 - mo�nost pou��t� seznamu proxy server�
 - historie stahov�n�
 - inteligentn� sledov�n� schr�nky
 - automatick� kontrola existence soubor� (funk�nosti odkaz�) na vzd�len�m serveru
 - automatick� aktualizace plugin�
 - automatick� ukon�en�
 - mnoho u�ivatelsk�ch nastaven� k p�izp�soben� zvyk�m u�ivatele
 - mo�nost spu�t�n� pod opera�n�mi syst�my Windows, Linux a MacOS
 - vypad� skv�le a funguje! :-)


R�zn�:
 - Drag&Drop URL adres
-  jednoduch� rozhran� pro programov�n� aplikac� (API) pro p�id�n� dal��ch slu�eb jako pluginy

V sou�asn� dob� jsou podporov�ny n�sleduj�c� slu�by (�azeno abecedn�):

 - 2shared.com                             - RapidShare.com
 - 4shared.com                             - RapidShare.com premium
 - badongo.com                             - rapidshare.de
 - bagruj.cz                               - rapidshareuser
 - bitroad.net                             - savefile.com
 - cobrashare.sk                           - sendspace.com
 - czshare.com                             - share-rapid
 - czshare.com_profi                       - shareator.com
 - dataup.de                               - shareonline.biz
 - depositfiles.com                        - stream.cz
 - easyshare.com                           - subory.sk
 - edisk.cz                                - tinyurl.com
 - egoshare.com                            - uloz.cz
 - enterupload.com                         - uloz.to
 - filebase.to                             - ulozisko.sk
 - filefactory.com                         - ultrashare.net
 - fileflyer.com                           - uploadbox.com
 - filesend.net                            - uploaded.to
 - flyshare.cz                             - uploading.com
 - hellshare.com                           - uploadjockey.com
 - hellshare.com_full                      - upnito.sk
 - hotfile.com                             - uppit.com
 - ifile.it                                - usercash.com
 - indowebster.com                         - webshare.net
 - iskladka.cz                             - wiiupload.net    
 - jandown.com                             - wikiupload.com
 - kewlshare.com                           - xtraupload.de
 - leteckaposta.cz                         - yourfiles.biz
 - letitbit.net                            - youtube.com
 - linkbucks.com                           - ziddu.com
 - load.to                                 - zippyshare.com
 - mediafire.com                           - zshare.net
 - megaupload.com
 - nahraj.cz
 - netload.in
 - paid4share.com
 - quickshare.cz
 - radikal.ru

Pozn.: Tento seznam nemus� b�t v�dy aktu�ln� - aktu�ln� seznam lze nal�zt na http://wordrider.net/freerapid

 II.    Syst�mov� po�adavky
=======================================

Doporu�en� konfigurace
    * Windows 2000/XP/Linux(j�dro 2.4)* nebo vy��� opera�n� syst�m
    * procesor Pentium 800MHz
    * minim�ln� rozli�en� obrazovky 1024x768
    * 40 MB voln� opera�n� pam�ti
    * 10 MB voln�ho prostoru na pevn�m disku
    * Java 2 Platform - nainstalovan� alespo� verze 1.6 (Java SE 6 Runtime)

Aplikace pro sv� spu�t�n� vy�aduje m�t nainstalovanou alespo� Javu 6.0 (http://java.sun.com/javase/downloads/index.jsp , JRE 6).

U�ivatel� Linuxu (Debian) like mohou pou��t tento p��kaz k instalaci Javy:
     sudo apt-get install sun-java6-jre

III.   Jak spustit FreeRapid
=======================================

Instalace
------------
Rozbalte archiv se soubory do libovoln�ho adres��e na pevn�m disku (cesta k aplikaci by nem�la obsahovat speci�ln� znaky typu '+', '!').
V p��pad� p�echodu na nov�j�� verzi sma�te p�edchoz� slo�ku aplikace. Ve�ker� u�ivatelsk� nastaven� jsou
zachov�na. Ulo�en� u�ivatelsk� nastaven� naleznete:
MS Windows: c:\Dokumenty a nastaven�\Va�e_u�ivatelsk�_jm�no\Data aplikac�\VitySoft\FRD
            a v registru HKEY_CURRENT_USER\Software\JavaSoft\Prefs\vitysoft\frd
Linux: ~/.FRD

NEKOP�RUJTE novou verzi programu p�es starou.

Pokud je pou�it p�ep�na� -p (viz d�le) je cesta ke konfigura�n�m soubor�m shodn� s adres��em programu.

Spu�t�n�
-----------
Windows
 Spus�te jednodu�e frd.exe

Linux
 Spus�te p��kazem ./frd.sh

V�echny OS platformy
 Spus�te p��kazem java -jar frd.jar


voliteln� parametry p�i spu�t�n�:

java -jar frd.jar [-h -v -d -D<property>=<value> -p]

volby
  -h (--help,-?)      tisk t�to zpr�vy
  -v (--version)      tisk informac� o verzi a ukon�en�
  -d (--debug)        tisk podrobn�j��ch informac� o b�hu programu
  -r (--reset)        reset u�ivatelsk�ch nastaven� do v�choz�ho stavu
  -m (--minim)        minimalizovat aplikaci po startu  
  -Dproperty=value    Nastaven� intern�ch hodnot vlastnost� (v�t�inou pro ��ely lad�n�, testov�n�)
  -p (--portable)     konfigura�n� soubory budou ulo�eny v adres��i 'config' u programu,
                      v�echny cesty (pokud je to mo�n�) budou ukl�d�ny relativn� v��i adres��i programu
                      - u�ite�n� nap�. pro USB FLASH disky


Pokud je hodnota volby -D nastavena na 'default' (bez '), pou�ije se v�choz� hodnota.

P��klad - spu�t�n� aplikace v lad�c�m m�du:
  Windows OS: frd.exe --debug
  Linux/MacOS: java -jar frd.jar --debug


V�ce informac�:
  - U�ivatelsk� p��ru�ka - http://wordrider.net/freerapid/help (lze tak� vyvolat stiskem kl�vesy F1 v programu)
  - Neofici�ln� tutori�l pro u�ivatele Linuxu - Jak nakonfigurovat FreeRapid Downloader na Linuxu (ve �pan�l�tin�)
    http://manualinux.my-place.us/freerapid.html


IV.    Zn�m� probl�my a omezen�
=======================================
- Aplikace se nespust�, jestli�e cesta k n� obsahuje speci�ln� znaky typu '+', '%'
  - X zm��te pros�m um�st�n� aplikace tak, aby cesta k n� neobsahovala speci�ln� znaky
- ESET "Smart" Antivirus na opera�n�m syst�mu Windows blokuje spu�t�n� FRD
  - X prove�te spr�vn� nastaven� va�eho antivirov�ho programu nebo spou�t�jte FRD t�mto zp�sobem: frd.exe -Doneinstance=false
- V�dy ��dn� ukon�ete FRD, jinak m��e doj�t ke ztr�t� Va�eho seznamu soubor� (eg. Windows shutdown with force option...)          
- V�b�r stahovan�ch soubor� ta�en�m my�� od shora dol� z n�jak�ho d�vodu zlob� :-(
    X vyb�rejte ��dky v tabulce pomoc� ctrl+kliknut� my�� nebo vyb�rejte polo�ky ta�en�m od sdola nahoru
- Substance look and feel vyhazuje v�jimku org.jvnet.substance.api.UiThreadingViolationException:
                                                     Component creation must be done on Event Dispatch Thread
    X ignorujte tuto v�jimku v souboru app.log
- V�jimka java.lang.UnsupportedClassVersionError
    X pravd�podobn� pou��v�te star�� verzi Javy (aplikace vy�aduje Sun Java verze 6 �i nov�j��)
- V�jimka IllegalArgumentException: 53687091 incompatible with Text-specific LCD contrast key
    X Je nutn� si spr�vn� nastavit (opravit) registry syst�mu Windows, v�ce viz (http://wordrider.net/forum/read.php?7,713,713#msg-713)     
- DirectoryChooser vyhazuje v�jimku java.lang.InternalError nebo zamrz� na Windows Vista (64bit)
    X ignorujte tuto v�jimku v souboru app.log
- java.lang.ClassCastException: java.awt.TrayIcon cannot be cast to java.awt.Component
    X ignorujte tuto v�jimku v souboru app.log    
- U�ivatel�m Linuxu se nezobrazuje ikona v oznamovac� oblasti
    X jedin�m �e�en�m tohoto probl�mu by mohl b�t p�echod JRE na verzi 1.6.0_10-rc �i nov�j��


V.    �e�en� probl�m�
=======================================
1. Nejprve si projd�te sekci IV kv�li zn�m�m chyb�m a omezen�m
2. Pokusil(a) jste se ji� aplikaci ukon�it a op�t spustit? :-)
3. Nav�tivte domovskou str�nku http://wordrider.net/freerapid a/nebo issue tracker na adrese http://bugtracker.wordrider.net/
   pro vlo�en� ozn�men� o nalezen� chyb�
4. M��ete se pokusit odstranit konfigura�n� soubory (jejich um�st�n� naleznete v sekci III - Instalace)
5. Spus�te aplikaci v lad�c�m m�du:
   Windows OS: frd.exe --debug
   Linux/MacOS: java -jar frd.jar --debug
6. Nahla�te probl�m dle pokyn� v sekci VI


VI.    Hl�en� chyb
=======================================
Pokud objev�te chybu, nahla�te mi ji co nejd��ve, aby mohla b�t opravena p�ed vyd�n�m dal�� verze.
K nahl�en� chyby  m��ete pou��t issue tracker (preferov�no), f�ra projektu �i m�j osobn� e-mail.

Napi�te pros�m Va�i verzi JRE a opera�n�ho syst�mu a p�ilo�te soubor app.log (naleznete jej ve slo�ce aplikace).
Usnadn� n�m to pr�ci p�i identifikaci probl�mu. Tak� n�m velmi pom��e, pokud spust�te aplikaci s parametrem --debug.
Ignorujeme jednoduch� hl�en� typu "Nejde mi spustit FRD. Co m�m d�lat?". Popi�te Va�i situaci a chov�n� aplikace.


issue tracker: http://bugtracker.wordrider.net/
f�rum: http://wordrider.net/forum/list.php?7
e-mail: bugs@wordrider.net (Va�e zpr�va m��e b�t odchycena spamov�m filtrem, proto tento zp�sob NEdoporu�uji!)


VII.    Jak podpo�it FreeRapid
=======================================
FreeRapid downloader je distribuov�n jako freeware. Pokud se V�m aplikace l�b� a r�di byste cht�li ocenit tv�rce
n�jakou finan�n� ��stkou za �as a zdroje vynalo�en� do v�voje , velmi n�s to pot��.
Jsme oby�ejn� studenti, kte�� mus� platit webhosting, ��ty za na�e p��telkyn� atd...

PayPal: http://wordrider.net/freerapid/paypal
   nebo
pou�ijte bankovn� ��et, jak je pops�no na domovsk� str�nce http://wordrider.net/freerapid/


VIII.   �asto kladen� ot�zky a odpov�di
=======================================

Q: Pro� jsi vytvo�il dal�� "RapidShare Downloader"?
A: 1) Proto�e nechci pou��vat rusk� software, kter� je pravd�podobn� pln� malwaru a spywaru.
   2) Proto�e si m��u s�m jednodu�e opravit automatick� stahov�n�.
   3) Proto�e ostatn� existuj�c� aplikace jsou u�ivatelsky nep��v�tiv� a �asto postr�daj� d�le�it� funkce.
   4) Proto�e m��u. :-)

Q: Jak zapnout podporu p��kaz� pro vyp�n�n� na Linuxu a MacOS?
A: Pro v�ce informac� nahl�dn�te pros�m do konfigura�n�ho souboru 'syscmd.properties', kter� naleznete ve slo�ce aplikace.

Q: Jak lze nastavit ��et pro Rapidshare premium?
A: Jako v�choz� je zapnut plugin pro FREE Rapidshare, proto je nutn� aktivovat plugin pro Premium.
Jd�te do Nastaven�->Mo�nosti->Pluginy -> panel Nastaven� , d�le nalezn�te plugin Rapidshare_premium a aktivujte ho (klikn�te v prvn�m sloupci ozna�en�m jako X).
D�le klikn�te na tla��tko mo�nost� pro zvolen� RapidShare_premium plugin a vlo�te autentifika�n� �daje. Potvr�te tla��tkem OK.
Podporov�no je pouze ��seln� u�ivatelsk� ID.

Q:U�ivatel� Windows - jak spustit program z p��kazov� ��dky?
A: Start->Spustit
cmd ENTER
cd cesta_k_adresari_freerapidu ENTER
frd.exe ENTER
pokud chcete napsat parametry tak nap�. frd.exe --debug 
