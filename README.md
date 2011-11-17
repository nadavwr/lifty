Lifty 1.7
=========

Getting started
---------------

Simply follow these steps

- Install SBT 0.11.1 (or any in the 0.11.x series)  
- Create a global plugin .sbt file: ~/.sbt/plugins/build.sbt
- Add the following to that file: addSbtPlugin("org.lifty" % "lifty" % "1.7.2")

Now you have lifty installed. So to create a new Lift project simple do this: 

- Create a new SBT project
- In the SBT console 
    - > lifty learn lift https://raw.github.com/Lifty/lifty/master/lifty-recipe/lifty.json
    - > lifty create lift project
    - > reload
    - > container:start
    - > container:stop

About 1.7
-----------

The nice thing about Lifty 1.7 is that templates are now stored online together with a 
description file (in json). This means that templates, arguments (and their default values) 
can be updated easily. This should make it a lot easier for the community to help maintain lifty :)

It's also a whole lot easier to extend lifty. You simply create some new templates and a json descriptor 
and you're up and running :)