temp_folder=gui_bundle_temp
git_repo="$1"
date=$(date '+%Y%m%d')
zip_name=nearest_neighbors_GUI_only_"$date".zip

mkdir ./"$temp_folder"
cp -r "$git_repo"/out ./"$temp_folder"
rm -r ./"$temp_folder"/out/artifacts
cp -r "$git_repo"/RDF_Resources ./"$temp_folder"/RDF_Samples
cp "$git_repo"/config ./"$temp_folder"
cp "$git_repo"/LICENSE.md ./"$temp_folder"
cp "$git_repo"/README.md ./"$temp_folder"
cp "$git_repo"/run_interface.sh ./"$temp_folder"

rm ./"$zip_name"
cd "$temp_folder"
zip -r ../"$zip_name" * 
cd ..
rm -rf ./"$temp_folder"

