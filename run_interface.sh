SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
. "$SCRIPTPATH"/config
"$jdk"/bin/java -classpath "$jena"/lib/*:"$javafx"/lib/:"$SCRIPTPATH"/out/production/Similarity_Search --module-path "$javafx"/lib --add-modules=javafx.fxml,javafx.controls implementation.gui.NeighborsInterface
