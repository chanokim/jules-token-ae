cd lib
CLASSPATH=`for i in *.jar; do echo -n "lib/$i:";done;`
cd ..

java -Xmx2000m -cp $CLASSPATH de.julielab.jtbd.TokenizerApplication $*
