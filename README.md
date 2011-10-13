Lifty 1.7
=========

Beta
---------

Please help me by trying out the beta. To do so try the following.

- Install SBT 10.1 
- Create a global plugin .sbt file: ~/.sbt/plugins/build.sbt
- Add the following to that file: libraryDependencies += "org.lifty" % "lifty_2.8.1" % "1.7-BETA"
- Create a new SBT project using SBT 10.1 
- run "lifty learn lift https://raw.github.com/Lifty/Lifty-engine/master/lifty-recipe/lifty.json"
- run "lifty create lift project"
- run "reload"
- run "jetty-run" 
- ...
- profit

About 1.7
-----------

The nice thing about Lifty 1.7 is that templates are now stored online together with a 
description file (in json). This means that templates, arguments (and their default values) 
can be updated easily. This should make it a lot easier for the community to help maintain lifty :)

It's also a whole lot easier to extend lifty. You simply create some new templates and a json descriptor 
and you're up and running :)