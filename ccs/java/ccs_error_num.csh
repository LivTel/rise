#!/bin/csh
# Script for printing out error numbers set in Java source files.
# Searches for lines such as:
#     object.setErrorNum(123);
# and prints:
#     123
# Syntax:
#     ccs_error_num <java source filenames>
if($#argv < 1) then
    echo "ccs_error_num <java source filenames>"
    exit
endif
~dev/bin/scripts/java_error_num.csh $argv |  grep -v Processing | grep -v NO_ERROR | sed "s/CcsConstants.CCS_ERROR_CODE_BASE+//g" | sort -n

