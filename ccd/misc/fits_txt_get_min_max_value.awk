#!/usr/xpg4/bin/awk -f
#!/bin/awk -f
# Awk script thats takes the data output from fits_get_data, and extracts the smallest and largest
# data values from it
BEGIN {
  FS = ","
  naxis1 = 0
  naxis2 = 0
  minvalue = 99999
  minvaluex = 0
  minvaluey = 0
  maxvalue = 0
  maxvaluex = 0
  maxvaluey = 0
}
 {
   if(FNR == 1) # reading naxis#
     {
       naxis1 = $1
       naxis2 = $2
     }
   else # reading data line
     {
       for(i=1;i<=NF;i++)
	 {
	   if($i < minvalue)
	     {
	       minvalue = $i
	       minvaluex = i
	       minvaluey = FNR
	       print "new minimum value:"minvalue" found at ("i","FNR")"
	     }
	   if($i > maxvalue)
	     {
	       maxvalue = $i
	       maxvaluex = i
	       maxvaluey = FNR
#	       print "new maximum value:"maxvalue" found at ("i","FNR")"
	     }
	 }
     }
}
END {
  print "array size:"naxis1"x"naxis2
  print "minimum value:"minvalue" at ("minvaluex","minvaluey")"
  print "maximum value:"maxvalue" at ("maxvaluex","maxvaluey")"
}
