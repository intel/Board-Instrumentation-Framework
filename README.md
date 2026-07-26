## THIS PROJECT IS ARCHIVED

Intel will not provide or guarantee development of or support for this project, including but not limited to, maintenance, bug fixes, new releases or updates. 
Patches to this project are no longer accepted by Intel.  
If you have an ongoing need to use this project, are interested in independently developing it, or would like to maintain patches for the community, please create your own fork of the project.  

contact: webadmin@linux.intel.com

> [!NOTE]
> A maintained fork is available at the following URL at the date of this writing:
> 
> https://github.com/PatrickKutch/Board-Instrumentation-Framework

# Board-Instrumentation-Framework
This project allows you to instrument and graphically display pretty much anything you want in a flexible way. 
It consists of 3 parts, the data collector (Minion) written in Python, which sends data over a UDP socket to the data broker and recorder called Oscar, also written in Python.  The last part is a Java FX application called Marvin, which receives data from Oscar and displays it via a library of highly configurable 'widgets'.

Here are a couple of my YouTube videos that make use of this framework:
https://www.youtube.com/watch?v=6UUFWZs-Sck
https://www.youtube.com/watch?v=NYI8BDv17Lw

Marvin is now a Java 10+ application.  If you need Java 8, checkout the JAVA_8 branch.




Take a look at the 200+ page BIFF Instrumenation Framework User Guide.pdf file for details: https://github.com/intel/Board-Instrumentation-Framework/blob/master/BIFF%20Instrumentation%20Framework%20User%20Guide.pdf.
