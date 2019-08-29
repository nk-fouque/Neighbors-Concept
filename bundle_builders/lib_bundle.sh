temp_folder=gui_bundle_temp
git_repo="$1"
date=$(date '+%Y%m%d')
zip_name=nearest_neighbors_lib_only_"$date".zip
version=1_0_0

mkdir ./"$temp_folder"
mkdir ./"$temp_folder"/lib/
jar cfv nearest_neighbors_"$version".jar "$git_repo"/out/production/Similarity_Search .
mv nearest_neighbors_"$version".jar ./"$temp_folder"/lib/
cp "$git_repo"/LICENSE.md ./"$temp_folder"
cp "$git_repo"/README.md ./"$temp_folder"

rm ./"$zip_name"
cd "$temp_folder"
zip -r ../"$zip_name" *
cd ..
rm -rf ./"$temp_folder"

