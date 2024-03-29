**************************************************************
*   FreeRapid Downloader                                     *
*      by Ladislav Vitasek aka Vity                          *
*   Website/Forum/Bugtracker: http://wordrider.net/freerapid *
*   Mail: info@wordrider.net - sugerencias                   *
*   Ultimo C�mbio: 25th October 2008                         *
**************************************************************
======================================
Contenido:
   I.   �Qu� es FreeRapid Downloader?
  II.   Requicitos de sistema
 III.   Como ejecutar FreeRapid
  IV.   Problemas conocidos y limitaciones
   V.   Solucionando Problemas
  VI.   Informar Errores
 VII.   Donar
VIII.   FAQ
======================================

I.    �Qu� es FreeRapid Downloader?
=======================================

FreeRapid downloader es un software de descarga sencillo escrito en Java, para gestionar las descargas desde Rapidshare y 
otros sitions de descarga de archivos

Caracteristicas Principales
 - soporte para descargas desde multiples servicios
 - descarga usando listas de proxys
 - historial de descarga
 - monitoreo del portapapeles
 - interface de programacion (API) para agregar otros servicios mediante plugins
 - autoapagado
 - trabaja en linux y MacOS

Misc.:
 - Arrastrar y Soltar URLs

Los serivicios soportados actualmente son:
 -  Rapidshare.com (para cuentas premium refierace al sitio)
 -  FileFactory.com
 -  Uploaded.to
 -  MegaUpload.com
 -  DepositFiles.com
 -  NetLoad.in
 -  Megarotic.com and Sexuploader.com
 -  Share-online.biz
 -  Egoshare.com
 -  Easy-share.com
 -  Letibit.net
 -  XtraUpload.de
 -  Shareator.com
 -  Load.to
 -  Iskladka.cz
 -  Uloz.t

 II.    Requicitos del sistema
=======================================

Recommended configuration:
    * Windows 2000/XP/Linux(core 2.4)* o posterior
    * Procesador Pentium 800MHz 
    * resulci�n m�nima 1024x768 
    * 40 MB de memoria RAM
    * 10 MB de espacio libre en el disco 
    * Java 2 Platform - version 1.6 (Java SE 6 Runtime) 

la aplicacion necesita por lo menos Java 6.0 para funcionar (http://java.sun.com/javase/downloads/index.jsp , JRE 6)


III.   Como ejecutar FreeRapid Downloader
=========================--==============

Instalaci�n
------------
Descomprima los archivos a cualquier directorio, pero cuidado con caracteres especiales (como  '+' o '!') en la ruta
si hace una actualizaci�n a una versi�n posterior, puede eliminar la carpeta anterior todas las configuraciones se 
almacenan en su directorio.�
MS Windows: c:\Documents and Settings\YOUR_USER_NAME\application data\VitySoft\FRD
            y en el registro: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\vitysoft\frd

Linux: ~/.FRD

NO COPIE UNA NUEVA VERSION A LA CARPETA DONDE ESTA UNA VERSION ANTERIOR

Ejecici�n
-----------
Windows
 Simplemente ejecute frd.exe

Linux
 Ejecute el comando ./frd.sh

Todas las plataformas
 Ejecute el comando: java -jar frd.jar

paramentros adicionales para ejecutar son:

java -jar frd.jar [-h -v -d -D<property>=<value>


opciones
  -h (--help,-?)      muestra este mensaje
  -v (--version)      muestra la informaci�n de la versi�n y termina
  -d (--debug)        muestra informaci�n de depuraci�n  
  -r (--reset)        reinicia las propiedades de los usuarios a sus valores por defecto
  -m (--minim)        minimiza la ventana principal al iniciar 
  -Dproperty=value    Pasa el la propiedad y su valor a la aplicaci�n (frecuentemente para propocitos de depuraci�n)

f value of option -D is set 'default' (without ') default value will be used.

ejemplo - ejecucion de la aplicacion en modo de depuraci�n
  Windows : frd.exe --debug
  Linux/MacOS: java -jar frd.jar --debug


Mas Informac�n:
  - Tutorial no oficial para usuarios - como confugurar FreeRapid Downloader en Linux (en espa�ol)
    http://manualinux.my-place.us/freerapid.html


    
IV.    Errores conocidos y limitaciones
========================================
- La aplicaci�n no iniciara si esta localizada en una ruta con caracteres especiales como "+" o "%"
   X Por favor, mueva la aplicaci�n a otra ubicaci�n sin tales caracteres
- ESET "Smart" Antivirus en Windows bloquea el inicio de la plicacion
   X Corrija las configuraciones de su programa antivirus o ejecute antivirus con la opci�n: frd.exe -Doneinstance=false 
- Siempre cierre FRD apropiadamente, o puede perder su lista de archivos (ejemplo apagar forzadamente Windows)
- Selecionar desde el final hasta el principio de la lista en la ventana principal mientras descarga parcialmente 
  desaparece :-(
   X seleccione la fila de la tabla hjaciendo ctrl+click del raton o seleccione los items desde el inicio hasta el final
- substance look and feel lanza una excepci�n org.jvnet.substance.api.UiThreadingViolationException:
						Component creation must be done on Event Dispatch Thread
   X ignore esta excepci�n en el app.log
- java.lang.UnsupportedClassVersionError exception
    X uste esta usando una versi�n antigua de java, usted debe usar la version 6 o posterior de Java
- DirectoryChooser lanza java.lang.InternalError o se congela en Windows Vista (64bit)
    X ignore esta excepci�n en el app.log
- java.lang.ClassCastException: java.awt.TrayIcon cannot be cast to java.awt.Component
    X ignore esta excepci�n en el app.log
- Usuarios de linux informan que no se esta mostrando un icono en la bandeja en linux
    X la �nica soluci�n para este problema podria ser una actualizaci�n del JRE a la version 1.6.0_10-rc o posterior

V.    Troubleshooting
=======================================
1. Revise la secci�n IV - para algun error conocido o limitaci�n
2. �Haz intentado salir de la aplicaci�n y volver a entrar? :)
3. Revisa la pagina http://wordrider.net/freerapid y/o el seguimiento de errores en http://bugtracker.wordrider.net/
   para un posible nuevo error conocido.
4. Puede intentar eliminar los archivos de configuraci�n (su ubicaci�n esta descrita en la secci�n VI -  Instalaci�n )  
5. Ejecute la aplicacion en modo de depuraci�n
   Windows OS: frd.exe --debug
   Linux/MacOS: java -jar frd.jar --debug
6. Informe el problema con el app.log como es descrito en la secci�n VI




VI.    Informe de errores
=======================================
Si usted encuentra algun error en la aplicaci�n, por favor no asuma que ya lo se, hagamelo saber tan pronto sea posible
para solucionarlo antes de la proxima liberaci�n, ya que mis recursos son limitados, no puedo hacer correcciones a errores
a versiones anteriores, para informar un error, usted puede usar el issue tracker (recomendado), foros del proyecto o mi 
correo personal.

Por favor, indique su version de JRE y de su sistema operativo y adjunte el archivo app.log (el cual esta en la carpeta de 
FreeRapid) esto puede ayudarnos a reconocer el problema. Usted puede Ayudarnos tambien si ejecuta la aplicaci�n con el 
parametro --debug. Siempre ignoramos preguntas como "No puedo ejecutar FRD. �Qu� puedo hacer?". Describa su situacion y el comportamiento de la aplicaci�n 

issue tracker: http://bugtracker.wordrider.net/
foros: http://wordrider.net/forum/list.php?7
mail: bugs@wordrider.net (su correo puede ser atrapado por el filtro antispam, asi que esta forma as� que NO 
prefiera esta forma)



VII.    Donaciones
=======================================
FreeRapid Downloader es distribuido como freeware, pero sio desea expresar su apreciaci�n por el tiempo y recursos que el
autor ha utilizado desarrollandolo, lo aceptaremos y aceptamos donaciones monetarias.
Somos estudianes y debemos pagar nuestro hosting, la cuenta para nuestras noviasm etc...

PayPal: http://wordrider.net/freerapid/paypal
   o
use el numero de cuenta descrito en nuestra pagina  http://wordrider.net/freerapid/



VIII.   FAQ
=======================================
Q: �Por qu� cre� otro "RapidShare Downloader"?
A: 1) Por que no quiero depender de un software ruso que probablemente est� lleno de malware y spyware.
   2) Por que puedo simplemente solucionar las descargas automaticas por mi mismo
   3) Por que otros programas existentes tienen interfaces poco intuitivas y pierden caracteristicas importantes.
   4) Por que Puedo.

Q: Como puedo habilitar el soporte para comandos de apagado en Linux y MacOS?
A: Por favor, vea el archivo de configuraci�n 'syscmd.properties' en el directorio de la aplicaci�n para mas detalles

