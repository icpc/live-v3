for i in *.received.txt ; do
  mv $i ${i/.received./.approved.}
done