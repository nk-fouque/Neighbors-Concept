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
### Notes
* Still some *TODO* flags for noticed discrepancies between the implementation's behavior and its supposed perfect behavior