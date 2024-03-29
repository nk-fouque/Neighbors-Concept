# Neighbors concept algorithm  

## Project  
Implement the algorithms described in **Answers Partitioning and Lazy Joins for Efficient QueryRelaxation and Application to Similarity Search** by *Sébastien Ferré*
in **Jena** A Java library for Semantic Web  
### Objectives
* Finish implementing the two algorithms
* Compare the results obtained by the early implementation made using OCaml
* Make all the necessary accommodations for the patch to be re-usable by other Jena users
* Create a Demo version, if possible in the form of a Graphical User Interface
### Resources
* https://hal.archives-ouvertes.fr/hal-01945454/document : Publication describing the algorithms
* https://jena.apache.org/documentation/javadoc/jena/ : Jena Documentation for Base RDF Graphs
* https://jena.apache.org/documentation/javadoc/arq/ : Jena Documentation for ARQ, the SPARQL Engine
* https://openjfx.io/javadoc/12/ : JavaFX Documentation
## User Documentation
### Graphical User Interface
* Execute `implementation.gui.NeighborsInterface.main()` (done by the run_interface.sh script)
* Find your RDF File
  * Click on the *Browse* button and find your RDF file in the file explorer      
  * **or** directly type the absolute path of the file on your system in the text field next to the button  
* Select the right format for your file
* Click on *Load RDF File*
* In the choice box that appears on the right, select the uri of the node you want to find the neighbors of
  * If there are too many possible nodes, you can narrow it down by typing part of the uri you want in the text field next to the *Filter* Button

### Redistributable
Jena and JavaFX are not included in this repository.  
To use the launcher scripts, change variables in `config` to set up the path to your JDK, Jena and JavaFX  
**This project was developed using JDK 12.0.1, Jena 3.12.0 and JavaFX 12.0.1**, make sure to use versions that are compatible with these  

Four datasets are included in the repository : 
* royal.ttl is a small sample dataset that describes basic familial links in the british royal family
* mondial.ttl, mondial.n3 and mondial-europe.n3 describe many geographical items (downloaded from https://www.dbis.informatik.uni-goettingen.de/Mondial/)

Downloads section on bitbucket (https://bitbucket.org/sebferre/conceptsofneighbours/downloads/) contains two "bundles" 
if you're only interested in specific uses and don't want to clone the whole repository  
* GUI_only : the compiled code and launcher script, you will still need to configure your paths in the ```config``` file
* lib_only : compiled code in an archive to use as module in java
### Main Class
* Every part of the main is explained in comments in the code
* The lines you might want to change are : 
  * The one setting up the String `filename` which is the absolute path of the file containing the RDF data
  * The one setting up  the String `format` change the format to the format of your file (see Jena Documentation for supported formats) 
  * The one setting up the String `uriTarget` which is the uri of the node you want to find the neighbors of
  * The few ones at the beginning, defining the log4j logging levels
## Current Version  
Javadoc at https://nk-fouque.github.io/Neighbors-Concept/allclasses-index.html
Run run_interface.sh to open interface

### Content
* Class `Main` gives an example use of the partition algorithm
  
* Class `NeighborsImplementation` for some static function useful to `Main`
##### package `algorithms`
* Classes `Partition` and `Cluster` for the first Algorithm described in the publication
* Classes `LazyJoin`, `MatchTreeRoot` and `MatchTreeNode` for the second Algorithm described by the publication
##### package `utils`
* Class `CollectionsModel` using Java collections to store informations about the Model, 
notably in Hashmaps, very useful for their computational speed to avoid using ARQ
* Classes `ListUtils`, `ElementUtils`, `TableUtils` containing useful static functions 
respectively for `java.util.List`, `org.apache.jena.sparql.syntax.Element` and `org.apache.jena.sparql.algebra.Table`
* Classes `Stopwatch` and `SingleStopwatchCollection` a very simple class of stopwatches to compensate not being able to run Async Profiler on my computer
* Other misc. classes, see Javadoc
##### package `gui`
MVC representation for the Interface Demo  
See classes Javadoc for details
### Anytime Implementation
#### Manual Interruption
Both implementations use a version that cuts the algorithm in case of OutOfMemoryError or under some action of the user
* The Main class intercepts the SIGINT signal, naturally sent by IntelliJ when you click on the stop button (the second time it sends SIGKILL)
* The Interface has a Button to stop the algorithm
  
When the algorithm is interrupted, it finishes the current run of `iterate()` and moves every clusters to neighbors. 
It considers the algorithm over, printing/displaying everything it was supposed to.
* In the Main class, the process then stops
* In the interface, the process can be launched for another node
#### TimeOut
The Main class uses a back thread that cuts the algorithm after a certain amount of time  
  
The Interface has a field to select a time limit for the algorithm (also done in a back thread), 
this limit cannot be set after starting the algorithm, the user will have to use the interrupt button
### Results
* Works perfectly with any item of royal
* Works with mondial at depth 1 or with timeout