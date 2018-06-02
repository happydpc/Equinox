package Tk::COMPARE_TXT_MX_vs_SML_CLASS_ISSY_024;

use strict;            # This PacKage Checks the Consistencies between MX mission and Single SR,MR,LR Missions for ISSY CLASS and NAMES! Part of Fatigue Spectra Validation Suite!
use POSIX qw(sin cos); # Inputs are the TXT files and MX and SML ConvTable in text format  #Excell  $issynum $coco $lcname - The only Difference between MX & MT should be the 1g cases only.
                       # This Tool can also compare old and new TXT versions and can correlate an old *stf to get a new one - a hidden capability for the MasterUser!
                       # This Tool is derived from "Compare_TXT_between_Deliveries_generate_NEW_STF_files_v2.4"                    
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
	$self->{tool_version}  = '2.4';
	
    if (@_)
      {
        my %extra = @_;
        @$self{keys %extra} = values %extra;
      }

    my $root                          = $self->{ROOT};
	open(GLOBAL_LOG, ">>". $root->{GLOBAL}->{GLOBAL_LOGFILE});
    $self->{date}                     = $root->{GLOBAL}->{DATE};
	$self->{print_all_warning}        = 1;
	
    $self->{loadcase_file_SMLONLY}        = $root->{COMPARE_MX_SML}->{SINGLE};
    $self->{loadcase_file_MIX}            = $root->{COMPARE_MX_SML}->{MIX};

    $self->{write_EXCEL_correlation}  = 1;     # Creates a Special_File consistent with Excel Line - so Copy&Paste is Possible in EXCEL 
    $self->{NEW_EXCEL_CONVTABLE}      = $root->{COMPARE_MX_SML}->{CONVTABLE_TXT};	# ConvTable in text format  #Excell  $issynum $coco $lcname, 	
	
    $self->{make_new_STF_file}        = 0;     # Creates a NEW *.stf based on Values from OLD *.stf
    $self->{OLD_STF_FILE}             = 'previous_old_STF_filename_here.stf';	# Values from this file are used for NEW *.stf

    $self->{do_correlation_below}     = 0;     #These issynum are directly known i.e $self->{issy_new_vs_old_directly}->{5263} =   2115;
    if ($self->{do_correlation_below} > 0) {$self->{issy_new_vs_old_directly_column_FILENAME} =   'User_file with 2 columns NEW OLD';}

    print GLOBAL_LOG "\n  * <<START>> Comparing TXT of MIX with SINGLE |$self->{loadcase_file_SMLONLY}|$self->{loadcase_file_MIX}|\n\n";
	unless ((-e $self->{loadcase_file_SMLONLY})  && (-e $self->{loadcase_file_MIX})) {print GLOBAL_LOG " *** Not all Files Exists!\n"; return;}
    $self->Start_Compare_TXT_MX_vs_SML_process($root);
	print GLOBAL_LOG "\n  * <<END>>   Completed Comparing TXT of MIX with SINGLE\n";
	close(GLOBAL_LOG);
  }






sub Start_Compare_TXT_MX_vs_SML_process
  {
    my $self = shift;
    my $root = shift;
	$self->{modified_log_out}         = $self->{loadcase_file_SMLONLY} .'.SML_vs_MIX.log';

	open(LOG,    "> $self->{modified_log_out}" );           
	&load_loadcase_SINGLE_data_into_structure($self);
	&load_loadcase_MIX_data_into_structure($self);
	&write_summary_loadcase_file($self);
	close(LOG);

	&load_manual_Correlated_SINGLE_vs_MIX_issy_columns($self)                         if ($self->{do_correlation_below} > 0);
	&load_old_stress_input_eid_data_and_write_new($self)                              if ($self->{make_new_STF_file} > 0);
	&write_EXCEL_correlation_of_ISSY_LCNUM_for_NEW_TXT_based_on_PREV_Delivery($self)  if ($self->{write_EXCEL_correlation} > 0);
  }
                                     




sub load_manual_Correlated_SINGLE_vs_MIX_issy_columns
  {
    my $self      = shift;
	open( INPUT, "< $self->{issy_new_vs_old_directly_column_FILENAME}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);
	
	foreach (@contents)
      {
	    my $line = $_;
	    if (($_  =~m/^\s*$/) || ($_ =~m/\#/))    { next;}
	    #if (($_  =~m/[a-z]/) || ($_ =~m/[A-Z]/)) { next;}
	    $line    =~ s/^\s*//g;
			
	    my @data    = split('\s+', $line); next unless (defined $data[1]);
		my $new = $data[0]; 
		my $old = $data[1]; 
	    $self->{issy_new_vs_old_directly}->{$new} =   $old;   # print GLOBAL_LOG "$_\n";	
	  }
   }

  

sub write_summary_loadcase_file
  {
    my $self      = shift;
    my $prev      = 00000;
	my $k         = 1;
	print LOG "#:ISSY:::CLASS:::NAME:::::::SINGLE::TXT::::::::::::::::::::::::::::::::::::::::::::";
	print LOG ":::::::::::::::::::::::::::::::::::::::::::::::::::MIX::TXT:::::: ::::::::::::::::::\n";
	
    foreach (@{$self->{ALLCODES_NEW}->{ALLCODES}})
      {
		my $code    = $_;
		my $length  = length($code);
		my $i       = 0;
  		my $match_issy   = 'NO';        
		my $match_name   = 'NO'; 
		
		if ($prev == $code)   {$k++;} else {$k = 1;}
		my $segment = $self->{LCASE_NEW}->{$code}->{segname}->{$k};
        my $issynum = $self->{LCASE_NEW}->{$code}->{issyno}->{$k};
		
		if (defined $self->{LCASE_OLD}->{$code}->{issyno}->{$k})
			{
	           my $segment_old = $self->{LCASE_OLD}->{$code}->{segname}->{$k};
               my $issynum_old = $self->{LCASE_OLD}->{$code}->{issyno}->{$k};	

			   if ($issynum == $issynum_old)   {$match_issy = 'YES';}
			   if ($segment =~m/$segment_old/) {$match_name = 'YES';}
			   
			   my $line_old = 	$self->{LCASE_OLD}->{$code}->{line}->{$k};
			   my $line_new = 	$self->{LCASE_NEW}->{$code}->{line}->{$k};
			   print LOG sprintf("%6s %6s %6s |%-110s |%-110s %s","$match_issy","YES","$match_name","$line_new","$line_old","\n");
			}	
         else
			{
			   my $line_new = 	$self->{LCASE_NEW}->{$code}->{line}->{$k};
			   print LOG sprintf("%6s %6s %6s |%-110s | %s","$match_issy","NO","$match_name","$line_new","\n");
			}			    
			
		$prev = $code;		
      }
  }



sub load_loadcase_MIX_data_into_structure
  {
    my $self  = shift;
    my $i     = 0;
    my $j     = 0;
    my $type  = 'N';
	my $k = 1;
    open( INPUT, "< $self->{loadcase_file_MIX}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);
	
    @{$self->{LCASE_OLD}->{HEADER}} = ();
    $self->{LCASE_OLD}              = {};
    @{$self->{ALLCODES_OLD}->{ALLCODES}} = ();
    @{$self->{ISSYNUM_OLD}->{NUMBERS}}   = ();
	$self->{issy_code_array_unique_old}      = {};
	
    foreach (@contents)
      {
	my $line  = $_;
	if ($_ =~m/^\s*$/) {print GLOBAL_LOG " ** Spaces in SML file |$_|\n"; next;}
	if ($_ =~m/^\s*#/) {push(@{$self->{LCASE_OLD}->{HEADER}}, $_);    next;}
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
	elsif($all[0] =~m/START/)
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 3) {shift(@all);};
	    @array   = @all; 
	  }
	else
	  {
	    $segname = 'segment_' . $i;
	    @array   = @all; 
	  }
	
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
	if ($code =~m/0$/)   {$self->{STEADY_OLD}->{$code}    = $issynum;}
	if ($code =~m/000$/) {$self->{DPCASE_OLD}->{$mission} = $issynum;}

	if (defined $self->{LCASE_OLD}->{$code}) {$k++;} else {$k = 1;}
	$self->{LCASE_OLD}->{$code}->{mission}->{$k} = $mission;
	$self->{LCASE_OLD}->{$code}->{issyno}->{$k}  = $issynum;
	$self->{LCASE_OLD}->{$code}->{code}->{$k}    = $code;
	$self->{LCASE_OLD}->{$code}->{segname}->{$k} = $segname;
	@{$self->{LCASE_OLD}->{$code}->{fac}->{$k}}  = @factors;
	$self->{LCASE_OLD}->{$code}->{line}->{$k}    = $line;	
	push(@{$self->{ALLCODES_OLD}->{ALLCODES}},  $code);
	push(@{$self->{ISSYNUM_OLD}->{NUMBERS}}, $issynum);
	push(@{$self->{issy_code_array_unique_old}->{$issynum}}, $code);
	$self->{issy_unique_numbers_old}->{$issynum} = $code;	
	$self->{code_unique_numbers_old}->{$code} = $issynum;
      }
    $self->{LCASE_OLD}->{TOTAL} = $i;
  }


sub load_loadcase_SINGLE_data_into_structure
  {
    my $self  = shift;
    my $i     = 0;
    my $j     = 0;
    my $type  = 'N';
	my $k = 1;
    open( INPUT, "< $self->{loadcase_file_SMLONLY}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);
	
    @{$self->{LCASE_NEW}->{HEADER}} = ();
    $self->{LCASE_NEW}              = {};
    @{$self->{ALLCODES_NEW}->{ALLCODES}} = ();
    @{$self->{ISSYNUM_NEW}->{NUMBERS}}   = ();
	
    foreach (@contents)
      {
	my $line  = $_;
	if ($_ =~m/^\s*$/) {print LOG "$_\n"; next;}
	if ($_ =~m/^\s*#/) {push(@{$self->{LCASE_NEW}->{HEADER}}, $_); print LOG "$_\n"; next;}
	$line      =~s/^\s*//;
	$j = 0;
	my @array   = ();
	my $segname;
                my @all;
	@all        = split('\s+', $line) if ($line =~m/\s+/);
                @all        = split('\t+', $line) if ($line =~m/\t+/);
                
	if ($all[0] =~m/EXC/)
	  {
	    $segname = shift(@all);  $segname =~s/\s*//g;  $segname =~s/\t*//g;
	    @array   = @all; 
	  }
	elsif($all[0] =~m/START/)
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 3) {shift(@all);};
	    @array   = @all; 
	  }
	else
	  {
	    $segname = 'segment_' . $i;
	    @array   = @all; 
	  }
	
	foreach (@array)
	  {
	    $array[$j] =~s/\s*//g; $array[$j] =~s/\t*//g;
	    $j++;
	  }
	
	for (0 .. 8) {if ((!defined $array[$_]) || ($array[$_] !~m/\d+/)) {$array[$_] = '-0-';}}
	
	my $code       = shift(@array);
	my $issynum    = shift(@array);
	my @factors    = @array;
	my $mission    = unpack('A1 A4', $code); #$issynum = $mission . $issynum;
		
	unless ((defined $code) && (defined $issynum)) {next;}
	$i++;
	if ($code =~m/0$/)   {$self->{STEADY_NEW}->{$code}    = $issynum;}
	if ($code =~m/000$/) {$self->{DPCASE_NEW}->{$mission} = $issynum;}

	if (defined $self->{LCASE_NEW}->{$code}) {$k++;} else {$k = 1;}
	$self->{LCASE_NEW}->{$code}->{mission}->{$k} = $mission;
	$self->{LCASE_NEW}->{$code}->{issyno}->{$k}  = $issynum;
	$self->{LCASE_NEW}->{$code}->{code}->{$k}    = $code;
	$self->{LCASE_NEW}->{$code}->{segname}->{$k} = $segname;
	@{$self->{LCASE_NEW}->{$code}->{fac}->{$k}}  = @factors;
	$self->{LCASE_NEW}->{$code}->{line}->{$k}    = $line;
	push(@{$self->{ALLCODES_NEW}->{ALLCODES}},  $code);
	push(@{$self->{ISSYNUM_NEW}->{NUMBERS}}, $issynum);
	$self->{issy_unique_numbers_new}->{$issynum} = $code;
      }
    $self->{LCASE_NEW}->{TOTAL} = $i;
  }




sub load_old_stress_input_eid_data_and_write_new # speed without limits
  {
    my $self         = shift;

    my $i            = 1;
    unless (-e $self->{OLD_STF_FILE}) {print GLOBAL_LOG " *** Old Loads File does not Exist!\n"; return;}
    print GLOBAL_LOG "  *  loading stresses from:        \= $self->{OLD_STF_FILE}\n";
    open( INPUT, "< $self->{OLD_STF_FILE}" );
    my @eid_data    = <INPUT>;
    chop(@eid_data);
    close(INPUT);

	$self->{fatigue_fac_all}    = 1;
	$self->{use_stress_column}  = 1;
    my @header                  = ();	
	if ($eid_data[0] =~m/[a-z]|^\s*$/i) {for (1 ..2) {push(@header, shift(@eid_data))}}        # Remove the first 2 lines!
	
    foreach (@eid_data)
      {
	my $line = $_;
	if (($_  =~m/^\s*$/) || ($_ =~m/\#/))    { next;}
	#if (($_  =~m/[a-z]/) || ($_ =~m/[A-Z]/)) { next;}
	$line    =~ s/^\s*//g;

	my @data    = split('\s+', $line);
	next unless (defined $data[1]);

	my $issynum    = $data[0];
	my $x          = $data[1];	my $y = 0; 	my $xy = 0;
	$y             = $data[2] if ((defined $data[2]) && ($data[2] !~m/[a-z]/i));
	$xy            = $data[3] if ((defined $data[3]) && ($data[3] !~m/[a-z]/i));
	$issynum       =~s/\s*//g;
	
	if ($self->{use_stress_column} == 1) {$y = 0; $xy = 0;}
	if ($self->{use_stress_column} == 2) {$x = 0; $xy = 0;}
	if ($self->{use_stress_column} == 3) {$x = 0;  $y = 0;}

	if ((defined $issynum) && (defined $x) && ($issynum !~m/[a-z]/i))
	  {
	    $i++;
	    $self->{PREV_STF}->{$issynum}->{X}   = $x  * $self->{fatigue_fac_all};
	    $self->{PREV_STF}->{$issynum}->{Y}   = $y  * $self->{fatigue_fac_all};
	    $self->{PREV_STF}->{$issynum}->{XY}  = $xy * $self->{fatigue_fac_all};
	    $self->{PREV_STF}->{ISSYNUM}->{$i}   = $issynum;
	  }
      }
	  
	# Write_New_STF File  
	my $file = $self->{OLD_STF_FILE};  $file =~s/\.stf$/\_NEW\.stf/;
	#my $file = $self->{OLD_STF_FILE};  $file =~s/MR529/MR111/;  #  $file =~s/\.stf$/\_CS\.stf/; 
	open( FILE,  "> $file"); 
	
    foreach (@header) {print FILE "$_\n";}
	
    foreach (sort keys %{$self->{issy_unique_numbers_new}})
      {
         my $issynum = $_;
         my $val     = 99999;
		 my $code = $self->{issy_unique_numbers_new}->{$issynum};
		  
		 if (defined $self->{issy_new_vs_old_directly}->{$issynum})         #These cases are USER directly correlated for NEW from OLD!
           {   
              my $oldnum = $self->{issy_new_vs_old_directly}->{$issynum};
		      $val = $self->{PREV_STF}->{$oldnum}->{X};
			  $self->{these_old_issy_is_seen_also_in_new}->{$issynum} = 1;
			  #print FILE sprintf("%-8s %10.2f%s","$issynum","$val","\n");
			  print FILE "$issynum	$val\n";
           }
		 elsif (defined $self->{code_unique_numbers_old}->{$code})         # Match the COdes
		   {
		      my $old_issy = $self->{code_unique_numbers_old}->{$code};
		      if (defined $self->{PREV_STF}->{$old_issy}->{X}) 
		       {
		           $val = $self->{PREV_STF}->{$old_issy}->{X};
			       $self->{these_old_issy_is_seen_also_in_new}->{$issynum} = 1;
			       #print FILE sprintf("%-8s %10.2f%s","$issynum","$val","\n");
                   print FILE "$issynum	$val\n";
		         }	   
		   }
	     elsif (defined $self->{PREV_STF}->{$issynum}->{X})                # Match directly using ISSYNUM
		   {
		      $val = $self->{PREV_STF}->{$issynum}->{X};
			  $self->{these_old_issy_is_seen_also_in_new}->{$issynum} = 1;
			  #print FILE sprintf("%-8s %10.2f%s","$issynum","$val","\n");
              print FILE "$issynum	$val\n";
		   }	   
         else
		   {			 
             my $code_new = $self->{issy_unique_numbers_new}->{$issynum};
	         my $name_new = $self->{LCASE_NEW}->{$code_new}->{segname}->{1};
	         print FILE sprintf("%-8s %10.2f  %32s         %-20s%s","$issynum","$val","Now in NEW!","$name_new","\n");
		   }
      }
	# These codes existed in OLD and are not used in NEW - Why?  
    foreach (sort keys %{$self->{issy_unique_numbers_old}})
      {
	     my $issynum = $_;
		 my $val     = 88888;
	     if (defined $self->{PREV_STF}->{$issynum}->{X}) {$val = $self->{PREV_STF}->{$issynum}->{X};}
         unless ((defined $self->{these_old_issy_is_seen_also_in_new}->{$issynum}) && ($self->{these_old_issy_is_seen_also_in_new}->{$issynum} == 1)) 
		    { # These ISSY numbers are not used in NEW TXT !
			   my $code_old = $self->{issy_unique_numbers_old}->{$issynum};
			   my $name_old = $self->{LCASE_OLD}->{$code_old}->{segname}->{1};
               if ($self->{print_all_warning} > 0) {print FILE sprintf("%-8s %10.2f  %-40s %-20s%s","$issynum","$val","Un-Used_by_NEW! Delete-from-OLD-STF!","$name_old","\n");}
			}
      }		 
    close(FILE);  
  }




sub write_EXCEL_correlation_of_ISSY_LCNUM_for_NEW_TXT_based_on_PREV_Delivery
  {
    my $self       = shift;
	my $lcnum      = 0;

    open( INPUT, "< $self->{NEW_EXCEL_CONVTABLE}");
    my @contents   = <INPUT>;
    chop(@contents);
    close(INPUT);	
	
    @{$self->{EXCEL}->{UNIQUE_ISSY}} = ();
	my @abc        = @contents;
    my ($line, $lcname, $issynum, $class);

    foreach (@contents) 
      {
	    if (($_ =~m/^\s*$/) || ($_ =~m/^\t*$/) || ($_ =~m/\#/)) {next;}
	    $line = $_;  $line =~s/^\s*//;

	    if ($line =~m/\[|\]/) 
		  {
			$line =~s/\s*//g; $line =~m/(.+)\[(.+)\](.+)/;
			$issynum = $1;
			$class   = $2;
			$lcname  = $3;
		  }
		else 
		  {
			($issynum, $class, $lcname) = split('\s+', $line);
		  }

	    $lcnum++;
	    $issynum =~s/\s*//g; $lcname  =~s/\s*//g;  $class   =~s/\s*//g;  
	
	    if ((defined $lcname) && (defined $issynum) && (defined $class))
	      {
	         unless ($class =~m/\//)
	            {
		          $self->{EXCEL}->{NAMES_EXCELL}->{$class}->{$issynum}   = $lcname;
		          push(@{$self->{EXCEL}->{ISSY_NUMBERS}}, $issynum);
		          push(@{$self->{EXCEL}->{CLASS_CODES}},  $class);
	            }
	         else
	            {
		          my @a = split('/', $class);
	             foreach (@a) 
		            {
		                my $clm = $_; $clm =~s/\s*//g; $clm =~s/\t*//g;
			            print GLOBAL_LOG "   * multi Loadcase per Code |$_|$issynum|$lcname|\n";
		                if (defined $self->{EXCEL}->{NAMES_EXCELL}->{$clm}->{$issynum}) {print GLOBAL_LOG " *** Overwriting LCNAME |$lcname|!\n";}
		                $self->{EXCEL}->{NAMES_EXCELL}->{$clm}->{$issynum}   = $lcname;
		                push(@{$self->{EXCEL}->{ISSY_NUMBERS}}, $issynum);
		                push(@{$self->{EXCEL}->{CLASS_CODES}},  $clm);
		            }
	            }
			 push(@{$self->{EXCEL}->{UNIQUE_ISSY}}, $issynum);
	      } # end if
	  } # end foreach loop
	
	my $file = $self->{NEW_EXCEL_CONVTABLE} . '_correlated.log';
    open(LOG2,    "> $file" );
	for (1 .. 2) {my $a = shift(@abc); print LOG2 "$a\n";}
	print LOG2 "# ISSY::::::::CODE::::::::::::NAME::::::::EXCEL CONVTABLE::::::::::::::::::::::::::ISSY:::CODE:::NAME::::::::::SINGLE_CONVTABLE:::::::::::::::::::MIX_CONVTABLE::::::::::::::::\n";
	my $prev_code = '00000';
	
    foreach (@{$self->{EXCEL}->{UNIQUE_ISSY}})	
	  {
	    my $issynum = $_;
	    my $x = 0;		
		foreach(@{$self->{EXCEL}->{ISSY_NUMBERS}})
		  {
		    my $n = $_;
		    if($issynum == $n) {last;}
		    $x++;
		  }
		my $code = @{$self->{EXCEL}->{CLASS_CODES}}[$x];
		my $name = $self->{EXCEL}->{NAMES_EXCELL}->{$code}->{$issynum};
		
		my $z = shift(@abc);
		print LOG2 sprintf("%-80s","$z");
		
		my $k = 1;
		if   ($prev_code == $code) {$k++;}
		else {$k = 1;}
		
		#for (1 .. 9)
		# {
		 #   my $k = $_;
  		    my $match_issy   = 'NO';        
		    my $match_name   = 'NO';			
		    if (defined $self->{LCASE_OLD}->{$code}->{issyno}->{$k})
			  {
			    my $segment_old = $self->{LCASE_OLD}->{$code}->{segname}->{$k};
                my $issynum_old = $self->{LCASE_OLD}->{$code}->{issyno}->{$k};	

			    if ($issynum == $issynum_old)   {$match_issy = 'YES';}
			    if ($name  =~m/$segment_old/)   {$match_name = 'YES';}
				if ($match_name eq "NO") {&check_if_steady_name_differences_is_caused_by_thermalized_steady($self, $name, $segment_old); $match_name = $self->{name_update};}

			    my $line_old = 	$self->{LCASE_OLD}->{$code}->{line}->{$k}; $line_old = unpack('A35', $line_old);
			    my $line_new = 	$self->{LCASE_NEW}->{$code}->{line}->{$k}; $line_new = unpack('A35', $line_new);
			    print LOG2 sprintf("|%6s %6s %6s |%-35s|%-35s|","$match_issy","YES","$match_name","$line_new","$line_old");
			  }
			else 
			 {
	if ($self->{print_all_warning} > 0) 
                {print LOG2 sprintf("|%6s %6s %6s |%-70s|","$match_issy","NO","$match_name","XXXXXXXX WARN XXXXXXX No Match for CODE |$code| - This CLASS Code/Event not Used/Exist before?");}
                else {print LOG2 sprintf("|%6s %6s %6s |%-70s|","$match_issy","NO","$match_name","");}
                if (defined $self->{issy_code_array_unique_old}->{$issynum}[0])
				   {  
				      print LOG2 "However IssyNum |$issynum| used in OLD TXT for ";		
				      foreach (@{$self->{issy_code_array_unique_old}->{$issynum}})
					     {
				           print LOG2 "|$_|$self->{LCASE_OLD}->{$_}->{segname}->{1}|";						 
						 }
				   }
             }  
        # }
		$prev_code = $code;
        print LOG2 "\n";		  
	  } # end foreach loop
	  
	close(LOG2);  
  }



sub check_if_steady_name_differences_is_caused_by_thermalized_steady
  {
    my $self       = shift;
	my $name_new   = shift;  
    my $name_old   = shift;

	$self->{name_update} = 'NO';
	my $gg_name_adapted = "GG";

	if (($name_old =~m/N\=1/) && ($name_old =~m/\_s\_/)) {$gg_name_adapted = $name_old; $gg_name_adapted =~s/\_s//;}
	if (($name_old =~m/N\=1/) && ($name_old =~m/\_t\_/)) {$gg_name_adapted = $name_old; $gg_name_adapted =~s/\_t//;}
	if (($name_old =~m/N\=1/) && ($name_old =~m/\_p\_/)) {$gg_name_adapted = $name_old; $gg_name_adapted =~s/\_p//;}
	#print LOG2 " $name_new | $name_old | $gg_name_adapted \n";	
	
	if ($gg_name_adapted  =~m/$name_new/)   {$self->{name_update} = 'YES';}
  }

  
  
  
  
  
  
1;    
