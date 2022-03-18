#!/bin/awk -f
# $Header: /space/home/eng/cjm/cvs/rise/scripts/icc_cshrc_edit.awk,v 1.1 2017-10-23 15:17:57 cjm Exp $
BEGIN{
  copy = 1
  nextcopy = 0
} 
/.*\<rise_install\:start\>.*/ { copy = 0 }
/.*\<rise_install\:end\>.*/ { nextcopy = 1 }
 { 
   if(copy == 1) {
     print $0
   }
   if(nextcopy == 1) {
     nextcopy = 0
     copy = 1 
   }
}
#
# $Log: not supported by cvs2svn $
#
