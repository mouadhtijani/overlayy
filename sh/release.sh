#!/bin/bash

banner='
  ___ _     ___    _    ____  _____
 |_ _| |   |_ _|  / \  |  _ \| ____|
  | || |    | |  / _ \ | | | |  _|
  | || |___ | | / ___ \| |_| | |___
 |___|_____|___/_/   \_\____/|_____|

  _____  ______ _      ______           _____ ______
 |  __ \|  ____| |    |  ____|   /\    / ____|  ____|
 | |__) | |__  | |    | |__     /  \  | (___ | |__
 |  _  /|  __| | |    |  __|   / /\ \  \___ \|  __|
 | | \ \| |____| |____| |____ / ____ \ ____) | |____
 |_|  \_\______|______|______/_/    \_\_____/|______|


'

echo "$banner"
while test $# -gt 0
do
	case "$1" in
		dry|d)
			dry=yes
			;;
		M|m|p)
			increase=$1
			;;
		minor)
			increase=m
			;;
		major)
			increase=M
			;;
		patch)
			increase=p
			;;
		--dry=*)
			dry=$(echo "$1" | sed s/--dry=//g | tr '[:upper:]' '[:lower:]')
			;;
		--help)
			echo "usage: [VERSION=(VERSION)] [NOM_RELEASE=(NOM_RELEASE)] release [M|m|p] [dry] [--dry=yes|no] [--help]"
			echo "  M|m|p           : increase the Major, minor or patch version"
			echo "  dry | --dry=yes : no modification are performed"
			echo " requires either VERSION to be set, or M or m or p parameter to be used as version increment"
			exit 0
			;;
		esac
	shift
done
# path pom.xml
pom_file="./../pom.xml"
if [ ! -f "$pom_file" ]; then
  echo "error : file $pom_file not found."
  exit 1
fi
# Find project version
#VERSION=A.B.C
oldVersion=$(grep -oP '<epi.version>\K.*?(?=<\/epi.version>)' "$pom_file")
echo "test1" $oldVersion
# Divide the version into major, minor, and patch parts.
IFS='.' read -ra version_parts <<< "$oldVersion"
major="${version_parts[0]}"
minor="${version_parts[1]}"
patch="${version_parts[2]}"

	case "$increase" in
		p)
			new_patch=$(expr $patch + 1)
			new_minor=$minor
			new_major=$major
			;;
		m)
			new_patch=0
			new_minor=$(expr $minor + 1)
			new_major=$major
			;;
		M)
			new_patch=0
			new_minor=0
			new_major=$(expr $major + 1)
			;;
		*)
			echo "invalid increase:$increase"
			exit 1
		;;
	esac
	VERSION="$new_major.$new_minor.$new_patch"
oldRelease=$(grep -oP '<epi.release>\K.*?(?=<\/epi.release>)' "$pom_file")
echo "test2" $oldRelease
# Extract the numeric part of the input value and remove leading zeros
numeric_part="${oldRelease:1}"
numeric_part="${numeric_part#"${numeric_part%%[!0]*}"}"

# Increment the numeric part
numeric_part=$((numeric_part + 1))

# Check if the numeric part exceeds 999
if ((numeric_part > 999)); then
    echo "Numeric part exceeded 999."
else
    # Format the numeric part with leading zeros
    formatted_numeric_part=$(printf "%03d" "$numeric_part")

    # Create the new value with the incremented numeric part
    new_value="R$formatted_numeric_part"
fi

echo "Last release with RELEASE_NUMBER=$oldRelease VERSION=$oldVersion [ $oldRelease $oldVersion ]"
echo "Making release for RELEASE_NUMBER=$new_value VERSION=$VERSION [ $new_value $VERSION ]"

## Ask the user if they want to continue
#read -p "Do you want to continue? (y/other): " choice
#
## Convert the choice to lowercase
#choice=${choice,,}
#
## Check if the choice is 'y' (yes)
#if [ "$choice" != "y" ]; then
#    echo "Script execution cancelled."
#    exit 0
#fi

#git checkout release/pre-release && git pull

# update porject version in pom
sed -i "s#<epi.version>.*</epi.version>#<epi.version>$VERSION</epi.version>#g" "$pom_file"
echo "La valeur de <epi.version> dans pom.xml a été mise à jour avec : $VERSION"
# update porject release in pom
sed -i "s#<epi.release>.*</epi.release>#<epi.release>$new_value</epi.release>#g" "$pom_file"
echo "La valeur de <epi.release> dans pom.xml a été mise à jour avec : $new_value"

# move postman collection into release directory
mkdir -p opencell-elec-postman/_releases/$new_value
if find "opencell-elec-postman/dev/" -maxdepth 1 -name '*.postman_collection.json' -print -quit | grep -q .; then
    mv opencell-elec-postman/dev/*.postman_collection.json opencell-elec-postman/_releases/$new_value/
fi
# move postman roles collection into release directory
mkdir -p opencell-elec-postman/roles/_releases_roles/$new_value
if find "opencell-elec-postman/roles/dev/" -maxdepth 1 -name '*.postman_collection.json' -print -quit | grep -q .; then
    mv opencell-elec-postman/roles/dev/*.postman_collection.json opencell-elec-postman/roles/_releases_roles/$new_value/
fi
# move files Postdeploy into release directory
mkdir -p opencell-elec-database/PostDeploy/_releases/$new_value
if find "opencell-elec-database/PostDeploy/dev/" -maxdepth 1 -name '*.sql' -print -quit | grep -q .; then
    mv opencell-elec-database/PostDeploy/dev/*.sql opencell-elec-database/PostDeploy/_releases/$new_value/
fi

# move files Predeploy into release directory
mkdir -p opencell-elec-database/PreDeploy/_releases/$new_value
if find "opencell-elec-database/PreDeploy/dev/" -maxdepth 1 -name '*.sql' -print -quit | grep -q .; then
    mv opencell-elec-database/PreDeploy/dev/*.sql opencell-elec-database/PreDeploy/_releases/$new_value/
fi

#generate postDeploy.sql
if find "opencell-elec-database/PostDeploy/_releases/$new_value/" -maxdepth 1 -name '*.sql' -print -quit | grep -q .; then
  sql_directory="opencell-elec-database/PostDeploy/_releases/$new_value/"
  out="opencell-elec-database/PostDeploy/generated/PostDeploy.sql"
  if [ -e "$out" ]; then
      rm "$out"
  fi
  for sql_file in "$sql_directory"*.sql; do
      if [ -f "$sql_file" ]; then
          echo "" >> "$out"
          filename=$(basename "$sql_file")
          echo "--PostDeploy/$new_value/$filename" >> "$out"
          cat "$sql_file" >> "$out"
          echo "" >> "$out"
      fi
  done
  # add postDeploy.sql to postDeploy-full.sql
  echo  "-----------------------------[$new_value]------------------------------------" >> "opencell-elec-database/FullDeploy/PostDeploy-full.sql"
  cat "$out" >> "opencell-elec-database/FullDeploy/PostDeploy-full.sql"
  echo "" >> "opencell-elec-database/FullDeploy/PostDeploy-full.sql" # Ajout d'un saut de ligne
fi

#generate PreDeploy.sql
if find "opencell-elec-database/PreDeploy/_releases/$new_value/" -maxdepth 1 -name '*.sql' -print -quit | grep -q .; then
  sql_directory="opencell-elec-database/PreDeploy/_releases/$new_value/"
  out="opencell-elec-database/PreDeploy/generated/PreDeploy.sql"
  if [ -e "$out" ]; then
      rm "$out"
  fi
  for sql_file in "$sql_directory"*.sql; do
      if [ -f "$sql_file" ]; then
          filename=$(basename "$sql_file")
          echo "" >> "$out"
          echo "--PreDeploy/$new_value/$filename" >> "$out"
          cat "$sql_file" >> "$out"
          echo "" >> "$out"
      fi
  done
  # add PreDeploy.sql to PreDeploy-full.sql
    echo  "-----------------------------[$new_value]------------------------------------" >> "opencell-elec-database/FullDeploy/PreDeploy-full.sql"
    cat "$out" >> "opencell-elec-database/FullDeploy/PreDeploy-full.sql"
    echo "" >> "opencell-elec-database/FullDeploy/PreDeploy-full.sql"
fi




# application et commit
#mvn install\
#  &&
#  git add .\
#   && git commit -am "release $VERSION"\
 #   && git tag $VERSION\
 #   && git push\
   #  && git push origin $VERSION\
   #    || exit 1