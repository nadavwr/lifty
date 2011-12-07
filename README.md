Lifty
=====

Lifty is a plugin that adds scaffolding to SBT 0.11.x (Simple Build Tool). It's easy to teach Lifty about new scaffolding templates, you just have to give it the url to recipe and it will download the templates and you're set. 

Getting started
---------------

You need to have SBT installed (any version of SBT in th 0.11.x series should do)

Now, add lifty as a global plugin by adding the following line to your `~/.sbt/plugins/build.sbt` file: 

    addSbtPlugin("org.lifty" % "lifty" % "1.7.4")

Whenever you're in a SBT session you now have the `lifty` command avaiable. The `lifty` command has tab-completion
enabled to make it faster/easier to use. Try typing `lifty` and hit tab in an SBT session.

Now that you have Lifty installed you need to teach it how to generate different files for you. You
do so by pointing lifty to a recipe. Here's a [list](http://lifty.github.com/ List) of all currently known recipes.

Here's what you would have to type to teach Lifty about Lift projects and templates 

    > lifty learn lift https://raw.github.com/Lifty/lifty/master/lifty-recipe/lifty.json

To create a fresh Lift webapp and start it simply type:

    > lifty create lift project
    > reload          
    > container:start
    > container:stop
    
Now point your browser to http://localhost:8080