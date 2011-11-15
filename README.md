Lifty 1.7
=========

Beta
---------

Please help me by trying out the beta. To do so try the following.

- Install SBT 11.x  
- Create a global plugin .sbt file: ~/.sbt/plugins/build.sbt
- Add the following to that file: addSbtPlugin("org.lifty" % "lifty" % "1.7")
- Create a new SBT project
- In the SBT console 
    - > lifty learn lift https://raw.github.com/Lifty/Lifty-engine/master/lifty-recipe/lifty.json
    - > lifty create lift project
    - > reload
    - > container:start
    - ... play around a bit 
    - > container:stop
- ...
- profit

About 1.7
-----------

The nice thing about Lifty 1.7 is that templates are now stored online together with a 
description file (in json). This means that templates, arguments (and their default values) 
can be updated easily. This should make it a lot easier for the community to help maintain lifty :)

It's also a whole lot easier to extend lifty. You simply create some new templates and a json descriptor 
and you're up and running :)