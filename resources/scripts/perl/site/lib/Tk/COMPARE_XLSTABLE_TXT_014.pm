package Tk::COMPARE_XLSTABLE_TXT_014;

use strict;            # This PacKage # Checks if the Loadcases in EXCELL Matches the Loadcases listed in TXT/CVT! Part of Fatigue Spectra Validation Suite!
use POSIX qw(sin cos); # ConvTable in text format  #Excell  $issynum $coco $lcname,    The only Difference between MX & MT should be the 1g cases only.
                       #It can only check the RANGE Missions - use another Tool for the Thermal Mix! Although you can use the MX to check Increments in MT. Yes a Trick to See if MX & MT are really compatible - should be the case!

sub new
  {
    my $type = shift;
    my $self = {};
    bless ($self, $type);
    $self->_init(@_);
    return $self;
  }


sub _init
  {
    my $self               = shift;
	$self->{tool_version}  = '1.2';
	
    if (@_)
      {
        my %extra = @_;
        @$self{keys %extra} = values %extra;
      }

    my $root                          = $self->{ROOT};
	my $wmai                          = $self->{MY_MISSION};
	open(GLOBAL_LOG, ">>". $root->{GLOBAL}->{GLOBAL_LOGFILE}); 	
	$self->{directory_path}           = $root->{GLOBAL}->{WORKING_DIRECTORY};    
    $self->{date}                     = $root->{GLOBAL}->{DATE};
	$self->{mission}                  = $root->{MISSION}->{$wmai}->{MISSION};
	print GLOBAL_LOG "  * |$self->{directory_path}|\n";
	
    $self->{loadcase_TXT_file}        = $root->{MISSION}->{$wmai}->{TXT_FILE};
    $self->{conversion_table}         = $root->{MISSION}->{$wmai}->{CONVTABLE_TXT};	# ConvTable in text format  #Excell  $issynum $coco $lcname, 
	
    print GLOBAL_LOG "\n  * <<START>> Comparing ConvTable with TXT |$wmai|$self->{conversion_table}|$self->{loadcase_TXT_file}|\n\n";
	unless ((-e $self->{conversion_table})  && (-e $self->{loadcase_TXT_file})) {print GLOBAL_LOG " *** Not all Files Exists!\n"; return;}
    $self->Start_Compare_XLS_TXT_process($root);
	print GLOBAL_LOG "\n  * <<END>>   Completed ConvTable with TXT\n";
	close(GLOBAL_LOG);
  }


  
sub Start_Compare_XLS_TXT_process
  {
    my $self = shift;
    my $root = shift;
    &load_loadcase_data_into_structure_count_only($self);
    &load_EXCELL_ConvTable_text_format($self);
	&write_summary_in_TXT_order_satisfaction($self);
  }



sub write_summary_in_TXT_order_satisfaction
  {
    my $self      = shift;
    my $prev      = 98765432210;
	my $k         = 1;
	my $outfile = $self->{loadcase_TXT_file} . '_compared_TXT_Excel_02.log';
    open(LOG,  ">".$outfile);
	print LOG "# Version $self->{tool_version} | $self->{date} | $self->{directory_path}|\n";
	print LOG "#:NAME:::CLASS:::ISSY::::::::::::TXT:::::::::::::::::::::::::::::::::::::::::::::::::::::::";
	print LOG ":::::::::::::::::::::::::::::::::::::::::EXCELL::::::::::::::::::::::::::::::::::::::::::::\n";
	
    foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
		my $code    = $_;
		my $length  = length($code);
		my $i       = 0;
		my $match_code   = 'NO';
        my $match_issy   = 'NO';
		my $match_name   = 'NO'; 
		#print LOG "$code  | prev: $prev|\n";
		if ($prev == $code)   {$k++;} else {$k = 1;}
		my $segment  = $self->{LCASE}->{$code}->{segname}->{$k};
        my $issynum  = $self->{LCASE}->{$code}->{issyno}->{$k};
		my $txt_line = 	$self->{LCASE}->{$code}->{line}->{$k};
		$self->{seen_these_issy_in_excel}->{$issynum} = 0;
		#print LOG " .................     |$code|$prev|$segment|$issynum|\n";
		
		if (defined $self->{NAMES_EXCELL}->{$code}->{$issynum})      # $self->{NAMES_EXCELL}->{$class}->{$issynum}   = $lcname;
		    {                                                        # $issynum, $class, $lcname
			   my $excel_name = $self->{NAMES_EXCELL}->{$code}->{$issynum};
			   my $excel_issy = $self->{ISSY_EXCELL}->{$excel_name}->{$code}->{$issynum};
			   my $excel_code = $self->{CLASS_EXCELL}->{$issynum}->{$excel_name}->{$code};
			   my $excel_line = $self->{LINE_EXCELL}->{$excel_issy};
			   if ($excel_name =~m/$segment/) {$match_name = 'YES';}
			   if ($excel_issy == $issynum)   {$match_issy = 'YES';}
			   if ($excel_code == $code)      {$match_code = 'YES';}
			   $self->{seen_these_issy_in_excel}->{$excel_issy} = 1;
			   
			   if ($match_name eq "NO") {&check_if_steady_name_differences_is_caused_by_thermalized_steady($self, $excel_name, $segment); $match_name = $self->{name_update};}
			   
			   print LOG sprintf("%6s %6s %6s |%-106s|%-100s| %s","$match_name","$match_code","$match_issy","$txt_line","$excel_line","\n");			   
			}
         else
			{
			   if (defined $self->{LINE_EXCELL}->{$issynum}) # Here the Excel Code is not same as TXT but the ISSY is the same?
			     {
				    my $excel_line = $self->{LINE_EXCELL}->{$issynum};  $match_issy = 'YES';
					my $excel_name = 'NA';
					my ($a, $b);
                    if ($length < 7) {($a, $b) = unpack('A1 A4', $code);}
					else {($a, $b) = unpack('A1 A6', $code);} 
					my @enames = ();
					for (1 .. 9)
					  {
					     my $ecode = $_ . $b;
					     if (defined $self->{NAMES_EXCELL}->{$ecode}->{$issynum}) 
						    {
							  $excel_name = $self->{NAMES_EXCELL}->{$ecode}->{$issynum}; push(@enames, $excel_name);
							  if ($excel_name =~m/$segment/) {$match_name = 'YES';}
							}
					  }
					my $t = $#enames + 1;  
				    print LOG sprintf("%6s %6s %6s |%-106s|[MatcH %i] %-55s|","$match_name","$match_code","$match_issy","$txt_line","$t","$excel_line");
					foreach (@enames) {print LOG sprintf("#%-24s#","$_");}
					print LOG "\n";
					$self->{seen_these_issy_in_excel}->{$issynum} = 1;
				 }
				else {print LOG sprintf("%6s %6s %6s |%-106s %s","$match_name","$match_code","$match_issy","$txt_line","\n");}	
			}			    
		$prev = $code;		
      }
	  print LOG ":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::\n";  
	  # These codes in EXCEL are not required !!!!! Why?
	  foreach (@{$self->{ISSY_NUMBERS}})
         {
	        my $excel_issy     = $_;
			#if (defined $self->{seen_these_issy_in_excel}->{$excel_issy})
			  {
			    unless((defined $self->{seen_these_issy_in_excel}->{$excel_issy}) && ($self->{seen_these_issy_in_excel}->{$excel_issy} == 1))
				  {
				     my $excel_line = $self->{LINE_EXCELL}->{$excel_issy};
				     print LOG "  *** Found in EXCEL but not Used in TXT/CVT -> Check ISSY |$excel_issy| #$excel_line# !\n";
				  }
              }
	     }
	  print LOG ":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::\n";	  
   close(LOG);	  
  }

  

sub check_if_steady_name_differences_is_caused_by_thermalized_steady
  {
    my $self        = shift;
	my $name_pure   = shift;  
    my $name_stp    = shift;

	$self->{name_update} = 'NO';
	my $gg_name_adapted = "GG";

	if (($name_stp =~m/N\=1/) && ($name_stp =~m/\_s\_/)) {$gg_name_adapted = $name_stp; $gg_name_adapted =~s/\_s//;}
	if (($name_stp =~m/N\=1/) && ($name_stp =~m/\_t\_/)) {$gg_name_adapted = $name_stp; $gg_name_adapted =~s/\_t//;}
	if (($name_stp =~m/N\=1/) && ($name_stp =~m/\_p\_/)) {$gg_name_adapted = $name_stp; $gg_name_adapted =~s/\_p//;}
	#print LOG2 " $name_pure | $name_stp | $gg_name_adapted \n";	
	
	if ($gg_name_adapted  =~m/$name_pure/)   {$self->{name_update} = 'YES';}
  }

  

sub load_EXCELL_ConvTable_text_format
  {
    my $self  = shift;
    my $lcnum = 0;
    my $file  = $self->{conversion_table};
	my $error = 0;
    my @loads = ();
    $self->{NAMES_EXCELL}     = {};
    @{$self->{ISSY_NUMBERS}}  = ();
    @{$self->{CLASS_CODES}}   = ();

    open(LOADS,  "<".$file);
    my @a = <LOADS>;
    chop(@a);
    close(LOADS);
	
    foreach (@a)
      {
        #chop($_) if ($_ =~m/\n$/);
        push(@loads, $_) if ($_ =~m/.+/);
		if ($_ =~m/.+\t+.+/)  {print GLOBAL_LOG " *** Tabs separated Data is unallowed in files! |$_|"; $error = 1; my $a = <STDIN>; return;}
      }

    my $outfile = $self->{loadcase_TXT_file} . '_compared_TXT_Excel_01.log';
    open(OUTPUT,  ">".$outfile);	  
    my ($line, $lcname, $issynum, $class);

    foreach (@loads) 
      {
	if (($_ =~m/^\s*$/) || ($_ =~m/^\s*\#/)) {next;}
	
	$line = $_;
	$line =~s/^\s*//;

	if ($line =~m/\[|\]/) # [1201211 / 1301111 / 1401111]
	  {
		#$line =~m/^(.+)\s+\[(.+)\]\s+(.+)?\s+(.+)$/;  # use when you have comments!
		$line =~m/^(.+)\s+\[(.+)\]\s+(.+)$/;  # use when you have only 3 columns!
		$issynum = $1;
		$class   = $2;
		$lcname  = $3; print OUTPUT "|$issynum|$class|$lcname|$line\n";
	  }
	else 
	  {
		($issynum, $class, $lcname) = split('\s+', $line);
	  }

	$lcnum++;
	$issynum =~s/\s*//g; 
	$lcname  =~s/\s*//g; 
	$class   =~s/\s*//g; 

	if ((defined $lcname) && (defined $issynum) && (defined $class))
	  {
	    unless ($class =~m/\//)
	      {
		if (defined $self->{NAMES_EXCELL}->{$class}->{$issynum}) {print GLOBAL_LOG "   ***           LCNAME |$lcname| i.e. Found more often than expected!\n";}  # OverWriting it!
		$self->{NAMES_EXCELL}->{$class}->{$issynum}   = $lcname;
		$self->{ISSY_EXCELL}->{$lcname}->{$class}->{$issynum}     = $issynum;
		$self->{CLASS_EXCELL}->{$issynum}->{$lcname}->{$class}             = $class;
		$self->{LINE_EXCELL}->{$issynum}              = $line;
		push(@{$self->{ISSY_NUMBERS}}, $issynum);
		push(@{$self->{CLASS_CODES}},  $class);
	      }
	    else
	      {
		my @a = split('/', $class);
		foreach (@a) 
		  {
		    my $clm = $_; $clm =~s/\s*//g; $clm =~s/\t*//g;
			print GLOBAL_LOG "   * multi Loadcase per Code |$_|$issynum|$lcname|\n";
		    if (defined $self->{NAMES_EXCELL}->{$clm}->{$issynum}) {print GLOBAL_LOG " ***          LCNAME |$lcname| i.e. Found more often than expected!\n";}  # OverWriting it!
		    $self->{NAMES_EXCELL}->{$clm}->{$issynum}   = $lcname;
			$self->{ISSY_EXCELL}->{$lcname}->{$clm}->{$issynum}     = $issynum;
			$self->{CLASS_EXCELL}->{$issynum}->{$lcname}->{$clm}           = $clm;
			$self->{LINE_EXCELL}->{$issynum}            = $line;
		    push(@{$self->{ISSY_NUMBERS}}, $issynum);
		    push(@{$self->{CLASS_CODES}},  $clm);
		  }
	      }
	  }
      }

    print OUTPUT "# Reference Files <$self->{loadcase_TXT_file}>  <$self->{conversion_table}>  <$self->{date}> \n";
    print OUTPUT "# !!!!!!!!!!    Search for NA in this File !!!!!!!!!!!!!!!!!\n";
    print OUTPUT "# LOADCASE.DAT            EXCELL CONVTABLE                   ISSY CLASS    NAME ISSY\n";

    foreach (@{$self->{ISSY_NUMBERS}})
      {
	my $issy     = $_;
	my $class    = shift(@{$self->{CLASS_CODES}});
	my $match    = 0;
	my $mnum     = 0;
	my $lcname   = $self->{NAMES_EXCELL}->{$class}->{$issy};
	my $txt_name = 'NA';

	#1
	if (defined $self->{LCASE}->{$class}->{segname})
	  {
	    $txt_name  = $self->{LCASE}->{$class}->{segname}->{1};
	    unless (defined $txt_name) {print OUTPUT "  ** In Excell is not defined a Name for TXT: |$lcname|$class|\n"; next;}
	    if ($txt_name =~m/$lcname/) 
	      {
		for (1 .. 2)
		  {
		    if (defined $self->{LCASE}->{$class}->{issyno}->{$_}) 
		      {
			my $txt_i = $self->{LCASE}->{$class}->{issyno}->{$_};
			if ($txt_i == $issy) {$mnum++;}
			$match++; #print OUTPUT "A\n";
		      }
		  }
	      }
	  }

	#eg.  EXC_SOMANR-01-1_F1_sup1
	if (($lcname =~m/[sup|inf]\d*$/) && ($match < 1))
	  {
	    my $c    = $lcname; $c =~s/\s*$//;
	    $lcname  =~m/(sup|inf)(\d+)$/;
	    my $a    = $c; $a =~s/_sup.*//;
	    my $b    = $c; $b =~s/_inf.*//;
	    #print OUTPUT "|$lcname|$a|$b|$c|\n";

	    if (defined $self->{LCASE}->{$class}->{segname})
	      {
		$txt_name  = $self->{LCASE}->{$class}->{segname}->{1};
		if ($txt_name =~m/$a|$b/)
		  {
		    for (1 .. 2) 
		      {
			if (defined $self->{LCASE}->{$class}->{issyno}->{$_})
			  {
			    my $txt_i = $self->{LCASE}->{$class}->{issyno}->{$_};
			    if ($txt_i == $issy) {$mnum++;}
			    $match++; #print OUTPUT "B\n";
			  }
		      }
		  }
	      }
	  }
	# TXT                               EXCEL  
	#EXC_N=1_-08f1_F2          EXC_N=1_-08_f1_F2
	
	if ($lcname =~m/PressLC/i)
	  {
	    if (defined $self->{LCASE}->{$class}->{segname}->{1})
	      {
		my $dp_name = $self->{LCASE}->{$class}->{segname}->{1}; #print "$issy $class $dp_name\n";
		if ($dp_name =~m/START/i) 
		  {
		    my $dp_i = $self->{LCASE}->{$class}->{issyno}->{1}; #print "$dp_i\n";
		    if ($dp_i == $issy) {$mnum++;}
		    $match++; #print OUTPUT "C\n";
		  }
	      }
	  }

	if ($txt_name eq 'NA')
	  {
	    $self->{also_txt_check}->{$txt_name}->{$issy}->{$class} = 0; 
	    print OUTPUT sprintf("%-25s %-30s %8s %-8s%4s%4s%s","$txt_name","$lcname","     ","$class","$match","$mnum","\n");
	  }
	elsif ($mnum == 0)
	  {
	    if (defined $self->{LCASE}->{$class}->{issyno}->{1}) 
	      {
		my $txt_issy = $self->{LCASE}->{$class}->{issyno}->{1};
		if ($txt_issy == $issy)  {$mnum++;}
		$self->{also_txt_check}->{$txt_name}->{$issy}->{$class} = 1;
		print OUTPUT sprintf("%-25s %-30s %8s %-8s%4s%4s%s","$txt_name","$lcname","$issy","$class","$match","$mnum","\n");
	      }
	  }	
	else
	  {
	    $self->{also_txt_check}->{$txt_name}->{$issy}->{$class} = 1;
	    print OUTPUT sprintf("%-25s %-30s %8s %-8s%4s%4s%s","$txt_name","$lcname","$issy","$class","$match","$mnum","\n");
	  }
      }
	
    print OUTPUT "\n___________ more follow-up for TXT contents __YES/NO_________\n";
	
    foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
	my $class_txt   = $_;
	my $i           = 0;
		
        if ($class_txt  =~m/000$/) 
	  {
	    my $issy_txt    = $self->{LCASE}->{$class_txt}->{issyno}->{1};
	    unless ($issy_txt == 1001) {print OUTPUT sprintf("%-30s%-9s%6s%45s%s","DPCASE","$class_txt","$issy_txt","Check Lcnum for DP NA issyNum to 1001?","\n");}
	    #next;
	  }	
	
        for (1 .. 99) # check all possibilities!
	  {
	    my $dir = $_;
            if (defined $self->{LCASE}->{$class_txt}->{segname}->{$dir})
	      {
		my $name_txt    = $self->{LCASE}->{$class_txt}->{segname}->{$dir};
		my $issy_txt    = $self->{LCASE}->{$class_txt}->{issyno}->{$dir};
		if (defined $self->{also_txt_check}->{$name_txt}->{$issy_txt}->{$class_txt})
		  {
		    my $m = $self->{also_txt_check}->{$name_txt}->{$issy_txt}->{$class_txt};
		    print OUTPUT sprintf("%-25s %8s %-8s %4s%s","$name_txt","$class_txt","$issy_txt","$m","\n");
		  }
		else
		  {
		    my $m = 'NA';
		    print OUTPUT sprintf("%-25s %8s %-8s %4s%s","$name_txt","$class_txt","$issy_txt","$m","\n");
		  }
	      }
	  }
      }
    print OUTPUT "_____________________________________________________\n";	
    close(OUTPUT);
  }




sub load_loadcase_data_into_structure_count_only
  {
    my $self  = shift;
    my $i     = 0;
    my $j     = 0;
    my $type  = 'N';
    my $k     = 1;
    $self->{esg_switch} = 0;

    open( INPUT, "< $self->{loadcase_TXT_file}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    @{$self->{LCASE}->{HEADER}} = ();
    $self->{LCASE}              = {};
    @{$self->{ALLCODES}->{ALLCODES}} = ();
    @{$self->{ISSYNUM}->{NUMBERS}}   = ();
	
    foreach (@contents)
      {
	my $line  = $_;
	if ($_ =~m/^\s*$/) {next;}
	if ($_ =~m/^\s*#/) {push(@{$self->{LCASE}->{HEADER}}, $_); next;}
	$line      =~s/^\s*//;
	$j = 0;
	my @array   = ();
	my $segname;
	my @all     = split('\s+', $line);
	if ($all[0] =~m/EXC/)
	  {
	    $segname = shift(@all);  $segname =~s/\s*//g;
	    @array   = @all; 
	  }
	elsif($all[0] =~m/START_OF_FLIGHT/i)      
	  {
	    $segname = shift(@all);  $segname =~s/\s*//g;
	    @array   = @all; 
	  }	  
	elsif($all[0] =~m/^START/i)      
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 3) {my $a = shift(@all);print STDERR "|$a|\n"; };
	    @array   = @all; 
	  }
	else
	  {
	    $segname = 'segment_' . $i;
		shift(@all);
	    @array   = @all; 
	  }
	                                                 # EXC_N=1_TXOT_F1      1GTXOT___4      11010 508 
	shift(@array) unless ($self->{esg_switch} < 1);  # Just for the A340/A330 ESG Project  !
	
	foreach (@array)
	  {
	    $array[$j] =~s/\s*//g;
	    $j++;
	  }
	
	for (0 .. 8) {if ((!defined $array[$_]) || ($array[$_] !~m/\d+/)) {$array[$_] = '-0-';}}
	
	my $code       = shift(@array);
	my $issynum    = shift(@array);
	my @factors    = @array;
	my $mission    = unpack('A1 A4', $code); #$issynum = $mission . $issynum;
		
	unless ((defined $code) && (defined $issynum)) {next;}
	$i++;
	if ($code =~m/0$/)   {$self->{STEADY}->{$code}    = $issynum;}
	if ($code =~m/000$/) {$self->{DPCASE}->{$mission} = $issynum;}
	if (defined $self->{LCASE}->{$code}) {$k++;} else {$k = 1;}     # find first time! & find other times!

	$self->{LCASE}->{$code}->{mission}->{$k} = $mission;
	$self->{LCASE}->{$code}->{issyno}->{$k}  = $issynum;
	$self->{LCASE}->{$code}->{code}->{$k}    = $code;
	$self->{LCASE}->{$code}->{segname}->{$k} = $segname;
	@{$self->{LCASE}->{$code}->{fac}->{$k}}  = @factors;
	$self->{LCASE}->{$code}->{line}->{$k}    = $line;	
	push(@{$self->{ALLCODES}->{ALLCODES}},  $code);
	push(@{$self->{ISSYNUM}->{NUMBERS}}, $issynum);
	if($code =~m/-0-/) {print STDERR "$line| $code|\n";}
      }
    $self->{LCASE}->{TOTAL} = $i;
  }



#sub activate_objscan
#  {
#    my $self  = shift;
#    my $mw    = MainWindow->new();
#    $mw->title("Data and Object Scanner");
#    my $scanner   = $mw->ObjScanner(
#				    caller => $self
#				   )->pack(
#					   -expand => 1
#					  );
#  }
  
  



1;  
