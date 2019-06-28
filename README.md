#Neighbors concept algorithm  

##Project  
Implement the algorithms described in **Answers Partitioning and Lazy Joins for Efficient QueryRelaxation and Application to Similarity Search** by *Sébastien Ferré*
in **Jena** A Java library for Semantic Web
###Objectives
* Finish implementing the two algorithms
* Compare the results obtained by the early implementation made using OCaml
* Make all the necessary accommodations for the patch to be re-usable by other Jena users
* Create a Demo version, if possible in the form of a Graphical User Interface
###Resources
* https://hal.archives-ouvertes.fr/hal-01945454/document : Publication describing the algorithms
* https://jena.apache.org/documentation/javadoc/jena/ : Jena Documentation for Base RDF Graphs
* https://jena.apache.org/documentation/javadoc/arq/ : Jena Documentation for ARQ, the SPARQL Engine
* https://openjfx.io/javadoc/12/ : JavaFX Documentation
## Current Version
### Content
* Classes `Partition` and `Cluster` for the partitioning Algorithm
* Class `Main` performing the algorithm
##### `utils` package
* Classes `ListUtils`, `ElementUtils`, `TableUtils` containing useful static functions 
respectively for `java.util.List`, `org.apache.jena.sparql.syntax.Element` and `org.apache.jena.sparql.algebra.Table`
* Class `CollectionsModel` using Java collections to store informations about the Model, 
notably in Hashmaps, very useful for their computational speed to avoid using ARQ when possible
* Class `RandomString` : self explaining Random String Generator user *erickson* on Stack Overflow 
(https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string)
* Classes `Stopwatch` and `SingleStopwatchCollection` a very simple class of stopwatches to compensate not being able to run Async Profiler on my computer
##### `gui` package
* MVC representation for the Interface Demo
  * `model` : empty because the actual model is the main class
  * `view`
    * `neighborsInterface.fxml` : fxml file for the base ground of the Interface
    * `css` : All the Stylesheets for the interface
  * `controller` 
    * `NeighborsController ` : JavaFX controller for the interface
#### Some explanations
##### 'keys' map

#####

### Notes
* Still some *TODO* flags for noticed discrepancies between the implementation's behavior and its supposed perfect behavior
### Results
* Works perfectly with any item of royal
* Works with mondial when limiting one object per predicate per subject in 160s
  * The only remaining problem factor is the size of the dataset which makes table joins very difficult
## User Documentation
#### Main Class
* Every part of the main is explained in comments in the code
* The lines you might want to change are : 
  * The one setting up the String `filename` which is the absolute path of the file containing the RDF data
  * In the one calling `read` change the format to the format of your file (see Jena Documentation for supported formats) 
  * The one setting up the String `uriTarget` which is the uri of the node you want to find the neighbors of
  * The few ones at the beginning, defining the log4j logging levels
  
#### GUI
* Execute `implementation.gui.NeighborsInterface.main()`  
* Find your RDF File
  * Click on the *Browse* button and find your RDF file in the file explorer      
  * **or** directly type the absolute path of the file on your system in the text field next to the button  
* Select the right format for your file
* Click on *Load RDF File*
* In the choice box that appears on the right, select the uri of the node you want to find the neighbors of
  * If there are too many possible nodes, you can narrow it down by typing part of the uri you want in the text field next to the *Filter* Button
