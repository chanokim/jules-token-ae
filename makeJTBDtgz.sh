## uncomment the build part in pom.xml

## before creating a tar-ball for distribution to third party, you might want to add some
## trained models (if so, add then into directory models)

# make binaries
mvn clean
mvn dependency:copy-dependencies
mvn package -Dmaven.test.skip=true
mkdir lib
rm lib/*
cp target/JTBD-2.4.jar lib/.
cp target/dependency/*.jar lib/.
rm lib/uima*.jar

# make documentation
cd doc
pdflatex JTBD.tex
pdflatex JTBD.tex
bibtex JTBD
pdflatex JTBD.tex
cp JTBD.pdf ../JTBD-2.4.pdf
cd ..


# do the packaging
tar -czvf JTBD-2.4.tgz runJTBDpackaged.sh src lib testdata resources \
README LICENSE COPYRIGHT JTBD-2.4.pdf \
--exclude=".svn" --exclude "*jules*java" --exclude "*src/test*" --exclude "julie*types.xml"

rm JTBD-2.4.pdf

