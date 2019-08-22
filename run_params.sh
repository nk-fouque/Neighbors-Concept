. ./config
"$jdk"/bin/java -classpath "$jena"/lib/*:"$javafx"/lib/:./out/production/Similarity_Search --module-path "$javafx"/lib --add-modules=javafx.fxml,javafx.controls implementation.ImplementationCommandLines "$@"
