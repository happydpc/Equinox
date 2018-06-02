package Tk::GENERATE_STH_PROFILE_076;

use strict;            # This PacKage generates the PROFILE information & ensures Consistencies check between ANA & TXT/CVT! Part of Fatigue Spectra Validation Suite!
use POSIX qw(sin cos ceil floor log10 atan);

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
	$self->{tool_version}  = '7.6';
	
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
	
    $self->{index_file}               = $root->{MISSION}->{$wmai}->{TXT_FILE};
    $self->{flugablauf_file}          = $root->{MISSION}->{$wmai}->{ANA_FILE};
    $self->{stress_input_file}        = $root->{MISSION}->{$wmai}->{STF_FILE};

	$self->{add2sth_Delta_P}          = $root->{GENERATE_STH}->{add2sth_Delta_P};              # 0 or 1 to use DP or not?
	$self->{reference_Delta_P}        = $root->{GENERATE_STH}->{reference_Delta_P};            # DP reference value (see conversion table for details)
	$self->{reference_Delta_T}        = $root->{GENERATE_STH}->{reference_Delta_T};            # Ref. Delta Temperature value
	$self->{overwrite_loadfile}       = $root->{GENERATE_STH}->{overwrite_loadfile};           # 0 to use existing Stresses.  1 to generate Dummy Stresses
	$self->{enable_SLOG_mode}         = $root->{GENERATE_STH}->{enable_SLOG_mode};             # Total Report Use for Debugging Only using 1D XXXXXXXXXXXXXX values!!!!!!!!
	$self->{esg_on}                   = $root->{GENERATE_STH}->{esg_on};                       # remove additional column for LR ESG only!
	$self->{rotation_degrees}         = $root->{GENERATE_STH}->{rotation_degrees};             # degrees - rotation angle?  Use 0³ for 1-D Spectra Inputs 
	$self->{use_stress_column}        = $root->{GENERATE_STH}->{use_stress_column};            # default is 0.  Can be Fixed for 1, 2, or 3rd Only Column!  However does not Allow Angle Rotation if  >  0 !!!!!!!!!!!!!!
	$self->{set_it_zero}              = $root->{GENERATE_STH}->{set_it_zero};                  # If YES/NO? (Yes will delete all negative Values from Spectra)
	$self->{fatigue_fac_all}          = $root->{GENERATE_STH}->{fatigue_fac_all};              # Multiply all Stresses with this Factor (also DP)!;
	$self->{delta_p_fac_only}         = $root->{GENERATE_STH}->{delta_p_fac_only};             # Multiply Factor applied Only on DP value.
	$self->{high_accuracy_of_results} = $root->{GENERATE_STH}->{high_accuracy_of_results};     # more digits  after the comma are printed in report file
	$self->{warn_if_li_nl_cases_comb} = $root->{GENERATE_STH}->{warn_if_li_nl_cases_comb};     # warns if > 0 that Linear and NonLinear cases are combined 
	$self->{run_only_till_flight_num} = $root->{GENERATE_STH}->{run_only_till_flight_num};     # default is 1e9  1e6! Yes really no Limit!
	
    $self->{make_zero_gg_incre}          = 'separated';   # options: special, none, steady, incre, [diff - auto +/- values] can be set to zero values! "separated" makes 1g diff from increments! 
    @{$self->{load_these_missions_only}} = ('1','2','3','4','5','6','7','8','9',); # only these missions will get loads

    $self->{force2run_with_missing_cases} =      0;     # default is 0. If set to 1, the job will run even if all your ISSY cases are not defined! Their values will be set to Zero!
    $self->{plaus_mission_preference}     =      1;     # Plausibility Check order of Missions. default is 1 (i.e 147258369) or 0 (i.e. 123456789). Assumed Missions 3,6,9 are the same!

    if   ($self->{plaus_mission_preference} == 1) {@{$self->{load_these_missions_only}} = ('1','4','7','2','5','8','3','6','9',); }  # STANDARD MECH & THERMAL
    elsif($self->{plaus_mission_preference} == 2) {@{$self->{load_these_missions_only}} = ('1','2','3','4','5','6','7','8','9',); }  # STANDARD MECH ONLY!
    elsif($self->{plaus_mission_preference} == 3) {@{$self->{load_these_missions_only}} = ('1','4','5','2','6','7','3','8','9',); }  # ONLY FLAP / SLAT -1000

    print GLOBAL_LOG "\n  * <<START>> Generate_Profile_Process |$wmai|$self->{date}|$self->{index_file}|$self->{flugablauf_file}|\n\n";
    $self->Start_Generate_STH_Profile_process($root);
	print GLOBAL_LOG "\n  * <<END>>   ProFile sth data generated! \n";
	close(GLOBAL_LOG);
  }




sub Start_Generate_STH_Profile_process
  {
     my $self  = shift;
	 my $root  = shift;

     if ($self->{add2sth_Delta_P} < 1) {$self->{reference_Delta_P} = 9876543219;}               

    $self->{eid}                      = $self->{stress_input_file};  
	if ($self->{eid} =~m/\..+$/i) {$self->{eid} =~s/\..+//;} else {$self->{eid} = $self->{eid} . '_x';}
    $self->{complete_validity}        = 0;
    $self->{max_stress_value}         = -1e10;
    $self->{min_stress_value}         =  1e10;
    $self->{max_stress_CODE}          = 'NA';
    $self->{min_stress_CODE}          = 'NA';
    $self->{flight_max_value}         =  0;
    $self->{flight_min_value}         =  0;
    $self->{switch_off_D2_error}      =  0;     # Do not Switch off (Leave it = 0) unless you know what you are doing!	
    unless ($self->{rotation_degrees} =~m/^\s*\d+\s*$/) {print GLOBAL_LOG " *** Rotation Angle required! For 1D use 0 degrees!"; return;}
	
    $self->{angle_radian}             = $self->{rotation_degrees} * 3.142 / 180.0;
    $self->{STEADY}                   = {};
    $self->{issy_unique_numbers}      = {};
    $self->{LCASE}                    = {};
    $self->{DPCASE}                   = {};
	$self->{PROFILE}                  = {};
	$self->{STEP_PRO}                 = {};
	if ($self->{overwrite_loadfile} < 1) {unless (-e $self->{stress_input_file}) {print GLOBAL_LOG " *** Rejected |$_| Stress InPut File not Found!\n"; next;}}
    unless ((-e $self->{index_file}) && (-e $self->{flugablauf_file})) {print GLOBAL_LOG " *** Rejected |$_| Not all Files Exists!\n"; next;}
	$self->{ERROR} = 0;
    &get_information_and_run($self, $root);
	&run_plausibility_checks_for_stf_pilot_point($self, $root);	
  }



sub get_information_and_run
  {
    my $self   = shift;
    my $eid    = $self->{eid};
    my $sth_file  = $self->{eid} .  '_HQ.sth';;
    my $report    = $self->{eid} .  '_HQ.rep';
    my $log       = $self->{eid} .  '_HQ.log';
    @{$self->{list_of_NL_cases}} = ();
	$self->{unique_nl_cases}     = {};
    open(OUTPUT,  ">" .$sth_file);
    open(REPORT,  ">" .$report);
    open(LOG,     ">" .$log);	
    print GLOBAL_LOG "  *  attempting to create STH to:  \= $sth_file\n";
    print OUTPUT " Spectra Generated by: HQX v $self->{tool_version} | $self->{date} | $self->{directory_path}|\n";
    print OUTPUT " CDF:|$self->{flugablauf_file}| CVT:|$self->{index_file}| STF:|$self->{stress_input_file}| StressColumn: $self->{use_stress_column}\n";
    print OUTPUT " 1-D Spectra - Not to be used for Analyses!  F_O: $self->{fatigue_fac_all} DP: $self->{delta_p_fac_only} Angle: $self->{rotation_degrees}\n";
    print OUTPUT " Calculation step:  1  Spectra Checks before Delivery\n";
    print REPORT " +++++++++ STH derivation ++++++++++++++++ \n";
	
    &load_loadcase_data_into_structure($self);
    &load_stress_input_eid_data($self);
    unless ($self->{ERROR} < 1) {print GLOBAL_LOG " *** Job rejected due to Errors! "; return;}
	
    &load_one_FT_Block_from_flugabluaf_and_process($self);
    print LOG "  * spectra validity: $self->{complete_validity} \n";
    print LOG "  * max_value: |$self->{max_stress_CODE}| $self->{max_stress_value} Flight_Num: $self->{flight_max_value}\n";
    print LOG "  * min_value: |$self->{min_stress_CODE}| $self->{min_stress_value} Flight_Num: $self->{flight_min_value}\n";

    foreach (@{$self->{list_of_NL_cases}})
      {
	my $a = unpack('A4', $_);
	$self->{unique_nl_cases}->{$a} = 1;
      }

    print LOG "NL Case List: ";
    foreach (sort keys %{$self->{unique_nl_cases}}) { print LOG "$_ "; }
    print LOG "\n";
	
	print LOG "::::::::::::::::::::: These IssyLC not used in ANA ::::::::::::::::::::::::::::::\n";
	for (1 .. $self->{$eid}->{ISSYNUM_TOTAL})
	  {
	    my $issy = $self->{$eid}->{ISSYNUM}->{$_};
		unless ($issy =~m/\d+/) {next;} 
	    if (defined $self->{$eid}->{$issy}->{seen_this_issy})
             {		
			  if ($self->{$eid}->{$issy}->{seen_this_issy} < 2) {print LOG "$issy  ";}
			 }
		else { print LOG "\n *** Very Strange Issy |$issy| \n";}
	  }
	print LOG "\n::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::\n";	  
    close(LOG);
    close(OUTPUT);
    close(REPORT);
  
    &create_flight_profile_min_mean_max($self);  
	&write_list_txt_sequence_loadcases_and_stresses($self); 
  }

  
 
sub create_flight_profile_min_mean_max
  {
    my $self      = shift;
	my $log       = $self->{eid} .  '_HQ.profile';
	open(LOG,     ">" .$log);
	print LOG "# SEGNAMES                CODE          MIN      MEAN     MAX       DP\n";
    foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
	   my $code    = $_;
	   my $segname = $self->{LCASE}->{$code}->{segname}->{1};
       if ($code   =~m/000$/) {next;}
	   next unless ($code =~m/0$/);
	   
       my $gg_code = $code;
	   my ($max, $min, $mean) = ('0','0','0');
	   
	   if (defined $self->{PROFILE}->{$gg_code}->{MEAN})
	     {
	       my $mean  = $self->{PROFILE}->{$gg_code}->{MEAN};
		   my $max   = $self->{PROFILE}->{$gg_code}->{MAX};
		   my $min   = $self->{PROFILE}->{$gg_code}->{MIN};
		   my $dp    = $self->{PROFILE}->{$gg_code}->{DP};
	       print LOG sprintf("%-25s %-10s %8.2f %8.2f %8.2f %8.2f","$segname","$gg_code","$min","$mean","$max","$dp");
		   
		   foreach (sort keys %{$self->{STEP_PRO}->{$gg_code}})
			 {
  			  my $PSTEP = $_; 
			  for (1 ..5)
				 {
				   my $CSTEP = $_;  
				   if (defined $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MAX})
					 {
					    my $mean  = $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MEAN};
			            my $smax  = $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MAX};
				        my $smin  = $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MIN};
			            print LOG sprintf("  %1i %1i %8.2f %8.2f %8.2f","$PSTEP","$CSTEP","$smin","$mean","$smax");
				     }
			      }
	         }
		   if (defined $self->{STEP_CONST_COMMENT}->{$gg_code}) {print LOG "    $self->{STEP_CONST_COMMENT}->{$gg_code}    $self->{STEP_CONST_VALUE}->{$gg_code}";}	 # e.g. for such comments 'TWPB_CONSTANT';
		   print LOG "\n";				 
         }			 
      }
    close(LOG);	  
  }

  
sub load_one_FT_Block_from_flugabluaf_and_process
  {
    my $self              = shift;
    my $root              = shift;
    my $file              = $self->{flugablauf_file};
    print GLOBAL_LOG "  *  generating sth data for eid:  \= $self->{eid}\n";

    open(FLUGAB,  "<" . $file);
    my $type   = 0;
    my $no     = -1;
    my $jump   = 0;

    while (<FLUGAB>)
      {
	chop($_);
	if (($_ =~m/^\s*$/) || ($_ =~m/^\s*\#/)) {print REPORT "$_\n";next;}

	my $line = $_;
	$line    =~s/^\s*//;

	if ($line =~m/^TF/)
	  {
	    $no++;
	    $type   = 1;

	    if ($no > 0)
	      {
             if ($no > $self->{run_only_till_flight_num}) {$jump = 1; last;};
	         &process_the_current_flugablauf_block($self,$no);
	      }
	    $self->{BLOCK}    = {};
	    $self->{BLOCK}->{HEAD} = $line;
	    next;
	  }
	elsif ($type > 2)
	  {
	    push(@{$self->{BLOCK}->{DATA}}, $line);
	    next;
	  }
	elsif ($type == 1)
	  {
	    $type++;
	    $self->{BLOCK}->{POINTS} = $line;
	    next;
	  }
	elsif ($type == 2)
	  {
	    $type++;
	    my ($valid, $block) = split('\s+', $line);
	    $self->{BLOCK}->{VALID} = $valid;
	    $self->{BLOCK}->{BLOCK} = $block;
	    next;	
	  }
      }
    $no++;
    &process_the_current_flugablauf_block($self,$no) unless($jump > 0);
    close(FLUGAB);
    #print GLOBAL_LOG "\n  * generated  stress time history data. \n";
  }



sub process_the_current_flugablauf_block
  {
    my $self     = shift;
    my $num      = shift;
    my $eid      = $self->{eid};
    my $i        = 0;

    my $points = $self->{BLOCK}->{POINTS};
    my $valid  = $self->{BLOCK}->{VALID};
    my $block  = $self->{BLOCK}->{BLOCK};
    my @data   = @{$self->{BLOCK}->{DATA}};
    my $real_points = $#data +1;
	my $this_flt_max = -100000;
	my $this_flt_min =  100000;
	my $this_flt_code_max = 0;
	my $this_flt_code_min = 0;	
    $self->{BLOCK}->{HEAD} =~m/TF_(.+)\s*\(/;
    my $header = $1;

    $self->{complete_validity} = $self->{complete_validity} + $valid * $block;

    print OUTPUT sprintf("%10.2f%10.2f %s",  "$valid","$block","\n");
    print OUTPUT sprintf("%10i%62s%-10s%s",  "$points","","$header","\n");
    print REPORT sprintf("%10i%5i %-100s%s", "$points","$num","$self->{BLOCK}->{HEAD}","\n");
    #print GLOBAL_LOG " TF_$num \n";

    foreach (@data)
      {
		my $line = $_;
		$line    =~s/^\s*//;
		my ($code,$coco,$dp,$temp) = split('\s+', $line);
		$self->{stress_value} = 0;
		&analyze_coco_fourteen_position($self, $coco, $dp, $temp);
		$i++;
		# stress in MPa
		if(($self->{stress_value} < 0) && ($self->{set_it_zero} =~m/YES/))
		  {
		    $self->{stress_value} = 0;
		  }
        print OUTPUT sprintf("%10.2f", "$self->{stress_value}") if  ($self->{high_accuracy_of_results} < 1);  
		print OUTPUT sprintf("%10.4f", "$self->{stress_value}") if (($self->{high_accuracy_of_results} > 0) && ($self->{high_accuracy_of_results} < 6));
		print OUTPUT sprintf("   % 5.3E",  "$self->{stress_value}") if  ($self->{high_accuracy_of_results} > 5);		
		if ($i == 8) {$i  = 0;	print OUTPUT "\n";}

		if ($self->{stress_value} > $self->{max_stress_value})
		  {
		    $self->{max_stress_value} = $self->{stress_value};
		    $self->{flight_max_value} = $num;
			$self->{max_stress_CODE}  = $line;
		  }
		if ($self->{stress_value} < $self->{min_stress_value})
		  {
		    $self->{min_stress_value} = $self->{stress_value};
		    $self->{flight_min_value} = $num;
			$self->{min_stress_CODE}  = $line;		
		  }
		if ($self->{enable_SLOG_mode} > 0) 
		  {
		    print REPORT sprintf("%12.4f %16s %-30s%s","$self->{stress_value}","$coco","$self->{long_list_loadcases}","\n")
		  }

		if ($self->{stress_value} > $this_flt_max)  {$this_flt_max = $self->{stress_value}; $this_flt_code_max = $line;}
		if ($self->{stress_value} < $this_flt_min)  {$this_flt_min = $self->{stress_value}; $this_flt_code_min = $line;}
      }
    unless ($i == 0) {$i  = 0; print OUTPUT "\n";}
		
    print LOG sprintf("TF:%5i   valid:%5i   block:%3i   Points:%8i ", "$num","$valid","$block","$points");
    if ($real_points != $points)  {print LOG " *** ERROR: TFnum: $num  Points_in_File: $points ! Use_Real_Points: $real_points \n";}
	print LOG sprintf("| max %8.2f    min: %8.2f | CodeMax %35s | CodeMin %35s |%s","$this_flt_max","$this_flt_min","$this_flt_code_max","$this_flt_code_min","\n");
  }

  
  
sub analyze_coco_fourteen_position
  {
    my $self   = shift;
    my $coco   = shift;
    my $dp     = shift;
    my $temp   = shift;
    my $eid    = $self->{eid};
    my $angle  = $self->{angle_radian};       # radians expected!
    my @a      = ();
    my $PSTEP  = 1;
	my $CSTEP  = 1;

    $coco      =~m/(\d\d\d\d)(\d\d)(\d\d)(\d\d)(\d\d)(\d\d)/;

    $a[0]      =  $1;
    $a[1]      =  $2;
    $a[2]      =  $3;
    $a[3]      =  $4;
    $a[4]      =  $5;
    $a[5]      =  $6;
    $dp        =  $dp * $self->{delta_p_fac_only}; # only on DP Value
    	
    if ((length($coco) < 14) || (length($coco) > 14))
      {
        print REPORT "\n *** Error |$coco| not standard length!\n";
        print LOG    "\n *** Error |$coco| not standard length!\n";
      }

    my $code     = shift(@a);
    my $gg_code  = $code . '0';
    my $issy_gg  = $self->{STEADY}->{$gg_code};
    my $x        = $self->{$eid}->{$issy_gg}->{X};  
    my $y        = $self->{$eid}->{$issy_gg}->{Y};
    my $xy       = $self->{$eid}->{$issy_gg}->{XY};
    my $i        = 0;

    unless (defined $self->{STEADY}->{$gg_code}) {print LOG " *** cannot inteprete: $code for $coco\n"; $self->{stress_value} = 99999999; return;}
    my $counter  = 0;
    my $ex_1     = 0;
    my $ex_2     = 0;

    my $mission  = unpack('A1 A4', $code);
    my $issy_dp  = $self->{DPCASE}->{$mission};
    my $dp_x     = $self->{$eid}->{$issy_dp}->{X}  * $dp / $self->{reference_Delta_P};
    my $dp_y     = $self->{$eid}->{$issy_dp}->{Y}  * $dp / $self->{reference_Delta_P};
    my $dp_xy    = $self->{$eid}->{$issy_dp}->{XY} * $dp / $self->{reference_Delta_P};

    $x  = $x  + $dp_x;
    $y  = $y  + $dp_y;
    $xy = $xy + $dp_xy;
    my $dp_val   = 0.5*($dp_x+$dp_y) + 0.5*($dp_x-$dp_y)*cos(2*$angle) + $dp_xy*sin(2*$angle); 	
    $self->{PROFILE}->{$gg_code}->{MEAN} = 0.5*($x+$y) + 0.5*($x-$y)*cos(2*$angle) + $xy*sin(2*$angle);	
    if ($self->{use_stress_column} == 1) {$self->{PROFILE}->{$gg_code}->{MEAN} = $x;   $dp_val = $dp_x;}
    if ($self->{use_stress_column} == 2) {$self->{PROFILE}->{$gg_code}->{MEAN} = $y;   $dp_val = $dp_y;}
    if ($self->{use_stress_column} == 3) {$self->{PROFILE}->{$gg_code}->{MEAN} = $xy;  $dp_val = $dp_xy;}	
    my $mean_1g = $self->{PROFILE}->{$gg_code}->{MEAN};	
    if ((defined $self->{PROFILE}->{$gg_code}->{DP}) && ($dp_val > $self->{PROFILE}->{$gg_code}->{DP})) {$self->{PROFILE}->{$gg_code}->{DP} = $dp_val;}
    else {$self->{PROFILE}->{$gg_code}->{DP} = $dp_val;}
	
    unless (defined $self->{$eid}->{$issy_dp}->{X}) {print LOG " *** cannot inteprete: $code for $coco\n"; $self->{stress_value} = 99999999; return;}
    my $list_segnames = sprintf("%21s[%6.1f]","$self->{LCASE}->{$gg_code}->{segname}->{1}","$mean_1g");
    $list_segnames    = $list_segnames . ' '  . sprintf("[DP%6.1f %6.2f]","$dp","$dp_x");
    #print GLOBAL_LOG " DP value: $dp   $coco\n";
    #print REPORT "$coco  lcnum: $issy_gg v: $x\n";
	
	if (defined $self->{$eid}->{$issy_gg}->{seen_this_issy}) {$self->{$eid}->{$issy_gg}->{seen_this_issy}++;}
    if (defined $self->{$eid}->{$issy_dp}->{seen_this_issy}) {$self->{$eid}->{$issy_dp}->{seen_this_issy}++;}

    foreach (@a)
      {
	$i++;
	my $ip        = $_;
	my ($n, $d)   = unpack('A1A1', $ip);
	my $incre     = $code . $i;
	my ($x_i,$y_i,$xy_i) = 0;
    my $relevant  = 0;
	if ($ip =~m/00/) {$ex_2++; next;}

	if (defined $self->{LCASE}->{$incre}->{issyno}->{$d})
	  {
	    my $issy = $self->{LCASE}->{$incre}->{issyno}->{$d};
	    my $fac  = @{$self->{LCASE}->{$incre}->{fac}->{$d}}[$n-1];
	    unless (defined $self->{LCASE}->{$incre}->{issyno}->{$d}) {print LOG " *** Loadcase Number not Found: $issy\n"; $self->{stress_value} = 99999999; return;}
	    $x_i     = $self->{$eid}->{$issy}->{X}  * $fac;
	    $y_i     = $self->{$eid}->{$issy}->{Y}  * $fac;
	    $xy_i    = $self->{$eid}->{$issy}->{XY} * $fac;       my $v = 0.5*($x_i+$y_i) + 0.5*($x_i-$y_i)*cos(2*$angle) + $xy_i*sin(2*$angle);
	    $ex_1++;
	    $x       = $x  + $x_i;
	    $y       = $y  + $y_i;
	    $xy      = $xy + $xy_i;
	    $counter++; 

	    my $name   = $self->{LCASE}->{$incre}->{segname}->{$d};  if ($name =~m/CSP0/) {$self->{STEP_CONST_VALUE}->{$gg_code} = $v; $self->{STEP_CONST_COMMENT}->{$gg_code} = 'TWPB_CONSTANT';}
		$relevant  = $self->{$eid}->{$issy}->{X}  if ($self->{use_stress_column} == 1);
		$relevant  = $self->{$eid}->{$issy}->{Y}  if ($self->{use_stress_column} == 2);
		$relevant  = $self->{$eid}->{$issy}->{XY} if ($self->{use_stress_column} == 3);		
	    $list_segnames = $list_segnames . ' ' .$name . sprintf("[%6.3fx%-6.1f%8.2f]","$fac","$relevant","$v");

	    my $seefac = abs($fac);
	    if ($seefac > 15) {print LOG sprintf("%24s %8s %20s %10s%s"," ** WARN CVT  L Factor: ","$fac","$coco","$incre","\n");}
	    #print REPORT "     LC1 <lcnum: $issy>  v: $x_i fac: $fac \n";
        if (defined $self->{$eid}->{$issy}->{seen_this_issy}) {$self->{$eid}->{$issy}->{seen_this_issy}++;}	
	  }

	elsif (($d == 2) && (!defined $self->{LCASE}->{$incre}->{issyno}->{2}))
	  {
	    if (defined $self->{LCASE}->{$incre}->{issyno}->{1}) 
	      {
		my $issy = $self->{LCASE}->{$incre}->{issyno}->{1};
		my $fac  = @{$self->{LCASE}->{$incre}->{fac}->{1}}[$n-1] * -1.0;

		$x_i     = $self->{$eid}->{$issy}->{X}  * $fac;
		$y_i     = $self->{$eid}->{$issy}->{Y}  * $fac;
		$xy_i    = $self->{$eid}->{$issy}->{XY} * $fac;   my $v = 0.5*($x_i+$y_i) + 0.5*($x_i-$y_i)*cos(2*$angle) + $xy_i*sin(2*$angle);
		$ex_1++;
		$x       = $x  + $x_i;
		$y       = $y  + $y_i;
		$xy      = $xy + $xy_i;
		$counter++;

		my $name = $self->{LCASE}->{$incre}->{segname}->{1};    if ($name =~m/CSP0/) {$self->{STEP_CONST_VALUE}->{$gg_code} = $v; $self->{STEP_CONST_COMMENT}->{$gg_code} = 'TWPB_CONSTANT';}
		$relevant  = $self->{$eid}->{$issy}->{X}  if ($self->{use_stress_column} == 1);
		$relevant  = $self->{$eid}->{$issy}->{Y}  if ($self->{use_stress_column} == 2);
		$relevant  = $self->{$eid}->{$issy}->{XY} if ($self->{use_stress_column} == 3);	
		$list_segnames = $list_segnames . ' ' .$name . sprintf("[%6.3fx%-6.1f%8.2f]","$fac","$relevant","$v");

		my $seefac = abs($fac);
		if ($seefac > 15) {print LOG sprintf("%24s %8s %20s %10s%s"," ** WARN CVT  L Factor: ","$fac","$coco","$incre","\n");}
		#print REPORT "     LC2 <lcnum: $issy>  v: $x_i fac: $fac\n";
        if (defined $self->{$eid}->{$issy}->{seen_this_issy}) {$self->{$eid}->{$issy}->{seen_this_issy}++;}			
	      }
	  }

	my $nl_incre = $code . $i . $ip;  my $nl_core = $code . $i;
	
	if (defined $self->{LCASE}->{$nl_incre}->{issyno}->{1})
	  {
	    my $issy = $self->{LCASE}->{$nl_incre}->{issyno}->{1};
	    my $fac  = @{$self->{LCASE}->{$nl_incre}->{fac}->{1}}[0];
	    #print REPORT "    NLC $coco <$nl_incre> <$issy> <$fac>\n";

	    if ($ip =~m/2$/) # Maxdam adds a -1 again to NLC of the 2nd direction!!!!!!!
	      {
        unless (defined  $self->{$eid}->{$issy}->{X}) {print LOG " *** Undefined ISSYnum |$coco|GG:$code|ISSY:$issy|DerivedNLCODE:$nl_incre|\n"; $self->{$eid}->{$issy}->{X} = 9876543210;}	
	    my $cifothernlc   =  0;
		my $max_extra_fac = -1;
        for	(1 .. 9) { my $nli = $nl_core . $_;  if(defined $self->{LCASE}->{$nli}->{mission} ) {$cifothernlc++;} }  # see MAXDAM rules for NL codes intepretation email sent 21/11/2011
		if ($cifothernlc > 2) {$max_extra_fac = 1;}
		$x_i     = $self->{$eid}->{$issy}->{X}  * $fac * $max_extra_fac;
		$y_i     = $self->{$eid}->{$issy}->{Y}  * $fac * $max_extra_fac;
		$xy_i    = $self->{$eid}->{$issy}->{XY} * $fac * $max_extra_fac;   
		if ($self->{switch_off_D2_error} < 1) {unless ($fac == -1) {print GLOBAL_LOG " *** Error |$nl_incre| factor NL D-2 not -1\n"; print LOG " *** Error |$nl_incre| factor NL D-2 not -1\n";}}
	      }
	    else
	      {
       unless (defined  $self->{$eid}->{$issy}->{X}) {print LOG " *** Undefined ISSYnum |$coco|GG:$code|ISSY:$issy|DerivedNLCODE:$nl_incre|\n"; $self->{$eid}->{$issy}->{X} = 9876543210;}
		$x_i     = $self->{$eid}->{$issy}->{X}  * $fac;
		$y_i     = $self->{$eid}->{$issy}->{Y}  * $fac;
		$xy_i    = $self->{$eid}->{$issy}->{XY} * $fac;   
	      }
	    $ex_1++;
	    if ($ex_1 > 1) 
		   {
		     unless ($self->{warn_if_li_nl_cases_comb} < 1) {print LOG " *** Error Linear and NL Cases Combined $coco \n";}
		   }
        my $v = 0.5*($x_i+$y_i) + 0.5*($x_i-$y_i)*cos(2*$angle) + $xy_i*sin(2*$angle);
	    $x       = $x  + $x_i;
	    $y       = $y  + $y_i;
	    $xy      = $xy + $xy_i;  
	    $counter++;

	    my $name = $self->{LCASE}->{$nl_incre}->{segname}->{1};
		$relevant  = $self->{$eid}->{$issy}->{X}  if ($self->{use_stress_column} == 1);
		$relevant  = $self->{$eid}->{$issy}->{Y}  if ($self->{use_stress_column} == 2);
		$relevant  = $self->{$eid}->{$issy}->{XY} if ($self->{use_stress_column} == 3);		
	    $list_segnames = $list_segnames . ' ' .$name . sprintf("[%6.3fx%-6.1f%8.2f]","$fac","$relevant","$v");

	    my $seefac = abs($fac);
	    if ($seefac > 1) {print LOG sprintf("%24s %8s %20s %10s%s"," ** WARN CVT NL Factor: ","$fac","$coco","$nl_incre","\n");}
	    #print REPORT "     NLC <$issy> $x_i fac: $fac\n";
        if (defined $self->{$eid}->{$issy}->{seen_this_issy}) {$self->{$eid}->{$issy}->{seen_this_issy}++;}			
	  }
	  
	# 1101 82 00 00 00 00   $ex_2  is the First Incre Position (i.e. 1-5).  $n is the Stepped Factor Code 82 (i.e. 1-9).
	if ($ex_1 > 0)  
	  {
	    $PSTEP = $n; $CSTEP = $i;    # $CSTEP is (coco pos. 1-5)   $n (fac. position 1-8) 
	    my $sv = 0;
	    $sv    = 0.5*($x+$y) + 0.5*($x-$y)*cos(2*$angle) + $xy*sin(2*$angle);
        $sv    = $x  if ($self->{use_stress_column} == 1);
        $sv    = $y  if ($self->{use_stress_column} == 2);
        $sv    = $xy if ($self->{use_stress_column} == 3);
		if (abs($relevant) > 0)
          {		
        if (defined $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MAX})  # EACH STEP MAX MIN
          {
            if ($sv > $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MAX}) {$self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MAX} = $sv;}
            if ($sv < $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MIN}) {$self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MIN} = $sv;}
            if ($self->{PROFILE}->{$gg_code}->{MEAN} > $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MEAN}) {$self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MEAN} = $self->{PROFILE}->{$gg_code}->{MEAN};} 
		  }
       else
          {
            $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MAX} = $sv;
	        $self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MIN} = $sv;
			$self->{STEP_PRO}->{$gg_code}->{$PSTEP}->{$CSTEP}->{MEAN} = $self->{PROFILE}->{$gg_code}->{MEAN};
          }
          }		  
	   }	  
      } # end of big loop 1-5 incre!

    $self->{stress_value} = 0.5*($x+$y) + 0.5*($x-$y)*cos(2*$angle) + $xy*sin(2*$angle);
    $self->{stress_value} = $x  if ($self->{use_stress_column} == 1);
    $self->{stress_value} = $y  if ($self->{use_stress_column} == 2);
    $self->{stress_value} = $xy if ($self->{use_stress_column} == 3);
	
    $self->{long_list_loadcases} = $list_segnames;

    if (($ex_1 < 1) && ($ex_2 < 5)) {print REPORT " *** ERROR $coco not interpreted!\n"; print LOG " *** ERROR $coco not interpreted!\n";}
    
    if (defined $self->{PROFILE}->{$gg_code}->{MAX})  # OVERALL MAX MIN
      {
        if ($self->{stress_value} > $self->{PROFILE}->{$gg_code}->{MAX}) {$self->{PROFILE}->{$gg_code}->{MAX} = $self->{stress_value};}
        if ($self->{stress_value} < $self->{PROFILE}->{$gg_code}->{MIN}) {$self->{PROFILE}->{$gg_code}->{MIN} = $self->{stress_value};}
		if ($self->{PROFILE}->{$gg_code}->{MAX_MEAN} < $self->{PROFILE}->{$gg_code}->{MEAN}) {$self->{PROFILE}->{$gg_code}->{MAX_MEAN} = $self->{PROFILE}->{$gg_code}->{MEAN};}
      }
    else
      {
        $self->{PROFILE}->{$gg_code}->{MAX} = $self->{stress_value};
	    $self->{PROFILE}->{$gg_code}->{MIN} = $self->{stress_value};
		$self->{PROFILE}->{$gg_code}->{MAX_MEAN} = $self->{PROFILE}->{$gg_code}->{MEAN};
      }
	  
  } # end sub



  
sub load_loadcase_data_into_structure
  {
    my $self  = shift;
    my $file  = $self->{loadcase_file};
    my $i     = 0;
    my $j     = 0;
    my $type  = 'N';
	my $k     = 1;

    open( INPUT, "< $self->{index_file}" );
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
	if ($all[0] =~m/EXC|N|\_/)
	  {
	    $segname = shift(@all);  $segname =~s/\s*//g;
		if ($self->{esg_on} > 0) {my $fem_lc = shift(@all);}
	    @array   = @all; 
	  }
	elsif($all[0] =~m/START/)
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 3) {shift(@all);};
		if ($self->{esg_on} > 0) {my $fem_lc = shift(@all);}
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

	if (length($code) > 5) {push(@{$self->{list_of_NL_cases}},$code);}
		
	unless ((defined $code) && (defined $issynum)) {next;}
	$i++;
	if ($code =~m/0$/)   {$self->{STEADY}->{$code}    = $issynum;}
	if (($code =~m/000$/) && (!defined $self->{DPCASE}->{$mission})) {$self->{DPCASE}->{$mission} = $issynum;}
	if (defined $self->{LCASE}->{$code}) {$k = $self->{LCASE_K}->{$code} + 1;} else {$k = 1;}     # find first time! & find other times!
	
	$self->{LCASE}->{$code}->{mission}->{$k} = $mission;
	$self->{LCASE}->{$code}->{issyno}->{$k}  = $issynum;
	$self->{LCASE}->{$code}->{code}->{$k}    = $code;
	$self->{LCASE}->{$code}->{segname}->{$k} = $segname;
    $self->{LCASE_K}->{$code}                = $k;
	@{$self->{LCASE}->{$code}->{fac}->{$k}}  = @factors;
	push(@{$self->{ALLCODES}->{ALLCODES}},  $code);
	push(@{$self->{ISSYNUM}->{NUMBERS}}, $issynum);
                
	     if ($code =~m/0$/) 
	       {
	          if (defined $self->{ALL_UNIQ_ISSY_NUMS}->{$issynum}) {print "  ERROR: ISSY NUMBERS used More than ONCE |$issynum|\n"; print LOG "  ERROR: ISSY NUMBERS used More than ONCE |$issynum|\n";}
	          if (defined $self->{ALL_UNIQ_CODES_NUMS}->{$code})   {print "  ERROR: CLASS CODE   used More than ONCE |$code|\n";    print LOG "  ERROR: CLASS CODE   used More than ONCE |$code|\n";}
	       }
	     $self->{ALL_UNIQ_ISSY_NUMS}->{$issynum} = 1;
	     $self->{ALL_UNIQ_CODES_NUMS}->{$code}   = 1;                
      } # end loop
    $self->{LCASE}->{TOTAL} = $i;
    &create_dummy_loadcase_list($self) if ($self->{overwrite_loadfile} > 0);
  }


sub write_list_txt_sequence_loadcases_and_stresses
  {
    my $self         = shift;
    my $eid          = $self->{eid};
    open( FILE,  ">". $self->{eid} ."_HQ.isy");
	
	print FILE "\n\n\n\n\n";
	my $prev = '9876543210';
	my $d    = 1;
    foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
	    my $code    = $_;
	    my $length  = length($code);

	    if ($prev == $code) {$d++;} else {$d = 1;}

        if((defined $self->{LCASE}->{$code}->{segname}->{$d}) && ($length == 5))
           {
             my $segname = $self->{LCASE}->{$code}->{segname}->{$d};
	         my $issynum = $self->{LCASE}->{$code}->{issyno}->{$d};
	         my $x       = $self->{$eid}->{$issynum}->{X};
	         my $y       = $self->{$eid}->{$issynum}->{Y};
	         my $xy      = $self->{$eid}->{$issynum}->{XY}; 
             print FILE sprintf("%-25s %-10s %-10s %10.2f %10.2f %10.2f%s","$segname","$code","$issynum","$x","$y","$xy","\n");				 
           }
		elsif ((defined $self->{LCASE}->{$code}->{segname}->{1}) && ($length > 5))  # NL Cases!	
		   {
		     my $segname = $self->{LCASE}->{$code}->{segname}->{1}; 
		     my $issynum = $self->{LCASE}->{$code}->{issyno}->{1};      
	         my $x       = $self->{$eid}->{$issynum}->{X};
	         my $y       = $self->{$eid}->{$issynum}->{Y};
	         my $xy      = $self->{$eid}->{$issynum}->{XY};
             print FILE sprintf("%-25s %-10s %-10s %10.2f %10.2f %10.2f%s","$segname","$code","$issynum","$x","$y","$xy","\n");					 
           }
         $prev = $code;
	   }
    close(FILE);	  
  }	

  
sub create_dummy_loadcase_list
  {
    my $self           = shift;
    #if(-e $self->{stress_input_file}) {return;}  # overwrite all the time !!!!!!
    my $i               = -1;
    my @issy_num_array  = @{$self->{ISSYNUM}->{NUMBERS}};
    my @cvt_codes_array = @{$self->{ALLCODES}->{ALLCODES}}; 
	
    open( FILE,  ">".$self->{stress_input_file}) or print " *** Stress Input File not Found |$self->{stress_input_file}|\n";
    print FILE "# Dummy Input for Spectra Checks \n";
    print FILE sprintf("%20s%s","1D","\n");
    
    $self->{issy_unique_numbers} = {};

    foreach (@issy_num_array)
      {
	my $issynum = $_;
	my $x       = 1 * 10;
	my $y       = rand(5) * 10;
	my $xy      = rand(1) *  1;
	$i++;
	my $code    = $cvt_codes_array[$i];	
	my $mission = unpack('A1 A4', $code);

	if ($self->{make_zero_gg_incre} =~m/steady/i)   {if     ($code =~m/0$/) {$x = $y = $xy = 0;}}
	if ($self->{make_zero_gg_incre} =~m/incre/i)    {unless ($code =~m/0$/) {$x = $y = $xy = 0;}}
	if ($self->{make_zero_gg_incre} =~m/special/i)  {unless ($code =~m/0$/) {$x = $y = $xy = 5;}}

	my $allow = 0;
	foreach (@{$self->{load_these_missions_only}})
	  {
	    my $mission_yes = $_;
	    if ($mission_yes =~m/$mission/) {$allow = 1;}
	  }

	unless (($allow > 0) && (@{$self->{load_these_missions_only}}[0] !~/all/i)) {$x = $y = $xy = 0;}
    $self->{issy_unique_numbers}->{$issynum} = $x;
	if ($self->{make_zero_gg_incre} =~m/diff|separated/i)
	  {
	     if ((defined $self->{codes_already_seen}->{$code}) && ($self->{codes_already_seen}->{$code} > 0))
		  {
		     unless ($self->{codes_already_seen}->{$code} == $issynum)		
			   {
			     my $f_1 = @{$self->{LCASE}->{$code}->{fac}->{1}}[0];
				 my $f_2 = @{$self->{LCASE}->{$code}->{fac}->{2}}[0];
				 if (($f_1 > 0) && ($f_2 > 0)) {$x = $x * -1.0;}
			   }
		  }
		 if ($self->{make_zero_gg_incre} =~m/separated/i) {$x       = 2 * 10;}
	  }
    $self->{codes_already_seen}->{$code}     = $issynum;	
	print FILE sprintf("%-10s %10.2f %10.2f %10.2f %10s%s","$issynum","$x","$y","$xy","$code","\n");
	#print FILE sprintf("%-10s %10.2f      %-10s%s","$issynum","$x","$code","\n");
      }
    close(FILE);
    
    open( FILE,  ">". $self->{eid} ."_issy_unique_list.log");    
	print FILE "# Unique ISSY codes from $self->{eid} \n                1D  \n"; 
    foreach (sort keys %{$self->{issy_unique_numbers}})
      {
         my $num = $_;
         my $val = $self->{issy_unique_numbers}->{$num};
         print FILE sprintf("%-10s %10.2f%s","$num","$val","\n");
      }
    close(FILE);
  }



sub load_stress_input_eid_data # speed without limits
  {
    my $self         = shift;
    my $eid          = $self->{eid};
    my @file         = ();
    my $i            = 0;
    unless (-e $self->{stress_input_file}) {print GLOBAL_LOG " *** Loads File does not Exist!\n"; return;}
    print GLOBAL_LOG "  *  loading stresses from:        \= $self->{stress_input_file}\n";
    open( INPUT, "< $self->{stress_input_file}" );
    my @eid_data    = <INPUT>;
    chop(@eid_data);
    close(INPUT);

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
	    $self->{$eid}->{$issynum}->{X}   = $x  * $self->{fatigue_fac_all};
	    $self->{$eid}->{$issynum}->{Y}   = $y  * $self->{fatigue_fac_all};
	    $self->{$eid}->{$issynum}->{XY}  = $xy * $self->{fatigue_fac_all};
	    $self->{$eid}->{ISSYNUM}->{$i}   = $issynum;
		$self->{$eid}->{$issynum}->{seen_this_issy}  = 1;
	  }
      }
	$self->{$eid}->{ISSYNUM_TOTAL}  = $i;
	
	foreach (@{$self->{ISSYNUM}->{NUMBERS}})  # from TXT/CVT File
	  {
	    my $issy_needed = $_;
		unless (defined $self->{$eid}->{$issy_needed}->{X})  
		   {
		      $self->{ERROR} = 1; 
			  print GLOBAL_LOG " *** Not Defined value for ISSYNUM: $issy_needed (Value set to ZERO to enable Run)\n"; 
              print LOG    " *** Not Defined value for ISSYNUM: $issy_needed (Value set to ZERO to enable Run)\n";
			  $self->{$eid}->{$issy_needed}->{X}  = 0;   # only to Force this to Run!
              $self->{$eid}->{$issy_needed}->{Y}  = 0;   # only to Force this to Run!
              $self->{$eid}->{$issy_needed}->{XY} = 0;   # only to Force this to Run!
		   }
        if ($self->{force2run_with_missing_cases} > 0) {$self->{ERROR} = 0;}  # only to Force this to Run!
		}
  }


sub run_plausibility_checks_for_stf_pilot_point # 
  {
    my $self         = shift;
    my $eid          = $self->{eid}; 
    open( FILE,  ">". $self->{eid} ."_HQ.plc");

	my $prev = '9876543210';
	my $d    = 1;
    foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
	    my $code    = $_;
	    my $length  = length($code);
        if ($code   =~m/000$/) {next;}
	    if ($prev  ==  $code) {$d++;} else {$d = 1;}

        if((defined $self->{LCASE}->{$code}->{segname}->{$d}) && ($length == 5))
           {
             my $segname = $self->{LCASE}->{$code}->{segname}->{$d};
	         my $issynum = $self->{LCASE}->{$code}->{issyno}->{$d};
			 my $mission = $self->{LCASE}->{$code}->{mission}->{$d};
	         my $x       = $self->{$eid}->{$issynum}->{X};
	         my $y       = $self->{$eid}->{$issynum}->{Y};
	         my $xy      = $self->{$eid}->{$issynum}->{XY}; 
			 
			 my ($gg4, $incre_position) = unpack('A4 A1', $code);
			 
			 if ($incre_position == 0) # 1g Position
			    {
				   $self->{PLAUS}->{$mission}->{P0}->{$gg4}->{segname} = $segname;
				   $self->{PLAUS}->{$mission}->{P0}->{$gg4}->{issy}    = $issynum;
				   if ($self->{use_stress_column} == 1) {$self->{PLAUS}->{$mission}->{P0}->{$gg4}->{value}   = $x;}
				   if ($self->{use_stress_column} == 2) {$self->{PLAUS}->{$mission}->{P0}->{$gg4}->{value}   = $y;}				   
				   if ($self->{use_stress_column} == 3) {$self->{PLAUS}->{$mission}->{P0}->{$gg4}->{value}   = $xy;}				   
				}
			 if ($incre_position == 1) # Incre Position 1 (e.g. VG)
			    {
				   $self->{PLAUS}->{$mission}->{P1}->{$gg4}->{segname} = $segname;
				   $self->{PLAUS}->{$mission}->{P1}->{$gg4}->{issy}    = $issynum;
				   if ($self->{use_stress_column} == 1) {$self->{PLAUS}->{$mission}->{P1}->{$gg4}->{value}   = $x;}
				   if ($self->{use_stress_column} == 2) {$self->{PLAUS}->{$mission}->{P1}->{$gg4}->{value}   = $y;}				   
				   if ($self->{use_stress_column} == 3) {$self->{PLAUS}->{$mission}->{P1}->{$gg4}->{value}   = $xy;}
				}
			 if ($incre_position == 2) # Incre Position 2 (e.g. LG)
			    {
				   $self->{PLAUS}->{$mission}->{P2}->{$gg4}->{segname} = $segname;
				   $self->{PLAUS}->{$mission}->{P2}->{$gg4}->{issy}    = $issynum;
				   if ($self->{use_stress_column} == 1) {$self->{PLAUS}->{$mission}->{P2}->{$gg4}->{value}   = $x;}
				   if ($self->{use_stress_column} == 2) {$self->{PLAUS}->{$mission}->{P2}->{$gg4}->{value}   = $y;}				   
				   if ($self->{use_stress_column} == 3) {$self->{PLAUS}->{$mission}->{P2}->{$gg4}->{value}   = $xy;}
				}
			 if ($incre_position == 3) # Incre Position 3 (e.g. VM)
			    {
				   $self->{PLAUS}->{$mission}->{P3}->{$gg4}->{segname} = $segname;
				   $self->{PLAUS}->{$mission}->{P3}->{$gg4}->{issy}    = $issynum;
				   if ($self->{use_stress_column} == 1) {$self->{PLAUS}->{$mission}->{P3}->{$gg4}->{value}   = $x;}
				   if ($self->{use_stress_column} == 2) {$self->{PLAUS}->{$mission}->{P3}->{$gg4}->{value}   = $y;}				   
				   if ($self->{use_stress_column} == 3) {$self->{PLAUS}->{$mission}->{P3}->{$gg4}->{value}   = $xy;}
				}
			 if ($incre_position == 4) # Incre Position 4 (e.g. TURN)
			    {
				   $self->{PLAUS}->{$mission}->{P4}->{$gg4}->{segname} = $segname;
				   $self->{PLAUS}->{$mission}->{P4}->{$gg4}->{issy}    = $issynum;
				   if ($self->{use_stress_column} == 1) {$self->{PLAUS}->{$mission}->{P4}->{$gg4}->{value}   = $x;}
				   if ($self->{use_stress_column} == 2) {$self->{PLAUS}->{$mission}->{P4}->{$gg4}->{value}   = $y;}				   
				   if ($self->{use_stress_column} == 3) {$self->{PLAUS}->{$mission}->{P4}->{$gg4}->{value}   = $xy;}
				}
			 if ($incre_position == 5) # Incre Position 2 (e.g. LAF)
			    {
				   $self->{PLAUS}->{$mission}->{P5}->{$gg4}->{segname} = $segname;
				   $self->{PLAUS}->{$mission}->{P5}->{$gg4}->{issy}    = $issynum;
				   if ($self->{use_stress_column} == 1) {$self->{PLAUS}->{$mission}->{P5}->{$gg4}->{value}   = $x;}
				   if ($self->{use_stress_column} == 2) {$self->{PLAUS}->{$mission}->{P5}->{$gg4}->{value}   = $y;}				   
				   if ($self->{use_stress_column} == 3) {$self->{PLAUS}->{$mission}->{P5}->{$gg4}->{value}   = $xy;}
				}				
			 unless (defined $self->{PLAUS_TEMP}->{$gg4}) {push(@{$self->{PLAUS_ARRAY}->{$mission}}, $gg4);}
			 $self->{PLAUS_TEMP}->{$gg4} = 1;
           }
		elsif ((defined $self->{LCASE}->{$code}->{segname}->{1}) && ($length > 5))  # NL Cases!	
		   {
		     my $segname = $self->{LCASE}->{$code}->{segname}->{1}; 
		     my $issynum = $self->{LCASE}->{$code}->{issyno}->{1};
			 my $mission = $self->{LCASE}->{$code}->{mission}->{1};
	         my $x       = $self->{$eid}->{$issynum}->{X};
	         my $y       = $self->{$eid}->{$issynum}->{Y};
	         my $xy      = $self->{$eid}->{$issynum}->{XY};

			 my ($gg4, $incre_position, $severity) = unpack('A4 A1 A2', $code);
			 
			 $self->{PLAUS}->{$mission}->{NL}->{$gg4}->{$incre_position}->{$severity}->{segname} = $segname;
			 $self->{PLAUS}->{$mission}->{NL}->{$gg4}->{$incre_position}->{$severity}->{issy}    = $issynum;
			 if ($self->{use_stress_column} == 1) {$self->{PLAUS}->{$mission}->{NL}->{$gg4}->{$incre_position}->{$severity}->{value}   = $x;}
			 if ($self->{use_stress_column} == 2) {$self->{PLAUS}->{$mission}->{NL}->{$gg4}->{$incre_position}->{$severity}->{value}   = $y;}				   
			 if ($self->{use_stress_column} == 3) {$self->{PLAUS}->{$mission}->{NL}->{$gg4}->{$incre_position}->{$severity}->{value}   = $xy;}			 
			 $self->{PLAUS}->{$mission}->{NL}->{$gg4}->{$incre_position}->{$severity}->{nlcode}  = $code;
             push(@{$self->{PLAUS}->{$mission}->{NL_SEV_ORD}->{$gg4}->{$incre_position}}, $severity);  # To remember the Severity order same as TXT!
			 unless (defined $self->{PLAUS_TEMP}->{$gg4}) {push(@{$self->{PLAUS_ARRAY}->{$mission}}, $gg4);}
			 $self->{PLAUS_TEMP}->{$gg4} = 1;
           }
         $prev = $code;
	   }
	   
	print FILE "\n# Comparison of STEADY & INCREMENTAL Cases in Missions\n";	   
	my @missions_possble  = @{$self->{load_these_missions_only}};
	my $longest_mission   = 1;
	my $longest_segnum    = 1;	
	
	foreach(@{$self->{load_these_missions_only}})
	  {
	    my $mission    = $_;
		if (defined $self->{PLAUS}->{$mission})
		   {
		      my $segs = $#{$self->{PLAUS_ARRAY}->{$mission}};			  
		      if ($segs > $longest_segnum) {$longest_segnum = $segs;  $longest_mission = $mission;}
			  print FILE "  * Mission < $mission > has <$segs +1> Steady Segments\n";
		   }
	  }
	$self->{PLAUS_SETTINGS}->{longest_mission} = $longest_mission;  
	
	&run_plausibility_checks_for_STEADY_values($self);
	&run_plausibility_checks_for_INCRE_Px_values($self);
	&run_plausibility_checks_for_NoN_LINEAR_INCRE_values($self);
    close(FILE);
  }


  

sub run_plausibility_checks_for_STEADY_values # 
  {
    my $self            = shift;
	my $longest_mission = $self->{PLAUS_SETTINGS}->{longest_mission};
	my $i = 0;
	
	print FILE "  *  n = Mission does not have this Steady Segment\n";
	print FILE "  *  x = Mission does not have this <Increment Position> in this Steady Segment\n\n";	
	
	for (1 .. 3)   # Run for the first 3 Missions
	 {
	   my $m = $_;
	   if ($m == $longest_mission) {print FILE "\nBEGIN_KEY: STEADY - Using Mission < $longest_mission > Segments as LONGEST MISSION!\n";	}  else {print FILE "\nBEGIN_KEY: STEADY - Using Mission < $m > Segments as Baseline!\n";	}
	   next unless (defined $self->{PLAUS}->{$m});
	   $i++;
       if ($i == 1) 
	     {
		   print FILE "                    MISSIONS  ";
		   foreach(@{$self->{load_these_missions_only}}) {print FILE sprintf("    M|%1s|","$_");}
		   print FILE "\n";
		 }
	   foreach(@{$self->{PLAUS_ARRAY}->{$m}})  # 1g codes are here in order as in TXT
	     {
	       my $gg4 = $_;
	       my ($a, $part) = unpack('A1 A3', $gg4);
		   my $segname    = $self->{PLAUS}->{$m}->{P0}->{$gg4}->{segname};
	       print FILE sprintf("%-21s |%4s|  ","$segname","$gg4");
		   foreach(@{$self->{load_these_missions_only}})
	         {
	            my $mission    = $_;
		        if (defined $self->{PLAUS}->{$mission})
		          {
		             my $gg4_now = $mission . $part;
			         my $value   = 'n';
				     if (defined $self->{PLAUS}->{$mission}->{P0}->{$gg4_now}->{value}) {$value = $self->{PLAUS}->{$mission}->{P0}->{$gg4_now}->{value}; print FILE sprintf("%8.2f","$value");} else {print FILE sprintf("%8s","$value");}
                     $self->{PLAUS_TEMP}->{$gg4_now} = 2;
			      }
				else
                  {
                     print FILE sprintf("%8s","n");
                  }				  
	         } 
            print FILE "\n";
	      } # end foreach	
        $i = 0;		  
	 }
	  
	foreach(sort keys %{$self->{PLAUS_TEMP}}) # Checks if we really considered everything #
	  {
	    my $gg4 = $_;
	    unless($self->{PLAUS_TEMP}->{$gg4} == 2) {print FILE "Ignored Segment: |$gg4|\n";}
	  }
	print FILE "\n\n\n";
  }



sub run_plausibility_checks_for_INCRE_Px_values ## 
  {
    my $self            = shift;
	my $longest_mission = $self->{PLAUS_SETTINGS}->{longest_mission};
	my $i = 0;
	
	for ('P1', 'P2', 'P3', 'P4', 'P5')  # These are the Linear Increments Positions 1,2,3,4,5.  
	  {
	    my $pos = $_;
		my $key = 'LINEAR_INCRE_' . $pos;
		
		for (1 .. 3)   # Run for the first 3 Baseline Missions since M1,M4,M7 should be same as M1, AND M2,M5,M8 same as M2, etc
		 {
		   my $m = $_;
		   if ($m == $longest_mission) {print FILE "\nBEGIN_KEY: $key - Using Mission < $longest_mission > Segments as LONGEST MISSION!\n";	}  else {print FILE "\nBEGIN_KEY: $key - Using Mission < $m > Segments as Baseline!\n";	}
		   next unless (defined $self->{PLAUS}->{$m});
		   $i++;
           if ($i == 1) 
		     {
			   print FILE "                    MISSIONS  ";
			   foreach(@{$self->{load_these_missions_only}}) {print FILE sprintf("    M|%1s|","$_");}
			   print FILE "\n";
			 }
		   foreach(@{$self->{PLAUS_ARRAY}->{$m}})  # 1g codes are here in order as in TXT
		     {
		       my $gg4 = $_;
		       my ($a, $part) = unpack('A1 A3', $gg4);
			   my $segname    = 'XxXxX';  # This segment may not have a Linear INCRE 
			   if (defined $self->{PLAUS}->{$m}->{$pos}->{$gg4}->{segname}) {$segname = $self->{PLAUS}->{$m}->{$pos}->{$gg4}->{segname};}
		       print FILE sprintf("%-21s |%4s|  ","$segname","$gg4");
			   foreach(@{$self->{load_these_missions_only}})
		         {
		            my $mission    = $_;
			        if (defined $self->{PLAUS}->{$mission})
			          {
			             my $gg4_now = $mission . $part;
				         my $value   = 'n';
						 if ($segname =~m/XxXxX/) {$value   = 'x';}
					     if (defined $self->{PLAUS}->{$mission}->{$pos}->{$gg4_now}->{value}) {$value = $self->{PLAUS}->{$mission}->{$pos}->{$gg4_now}->{value}; print FILE sprintf("%8.2f","$value");} else {print FILE sprintf("%8s","$value");}
                         $self->{PLAUS_TEMP}->{$gg4_now} = 2;
				      }
					else
                      {
                        print FILE sprintf("%8s","x");
                      }	  
		         } 
                print FILE "\n";
		      } # end foreach	
            $i = 0;		  
		 }
		print FILE "\n\n\n";	 
	 } 
	 
	foreach(sort keys %{$self->{PLAUS_TEMP}}) # Checks if we really considered everything #
	  {
	    my $gg4 = $_;
	    unless($self->{PLAUS_TEMP}->{$gg4} == 2) {print FILE "Ignored Segment: |$gg4|\n";}
	  }
	print FILE "\n\n\n";
  }




sub run_plausibility_checks_for_NoN_LINEAR_INCRE_values ###
  {
    my $self            = shift;
	my $longest_mission = $self->{PLAUS_SETTINGS}->{longest_mission};
	my $i = 0;
	
	my $key = 'NON_LINEAR_INCRE';
	
	for (1 .. 3)   # Run for the first 3 Missions
	 {
	   my $m = $_;
	   if ($m == $longest_mission) {print FILE "\nBEGIN_KEY: $key - Using Mission < $longest_mission > Segments as LONGEST MISSION!\n";	}  else {print FILE "\nBEGIN_KEY: $key - Using Mission < $m > Segments as Baseline!\n";	}
	   next unless (defined $self->{PLAUS}->{$m});
	   $i++;
       if ($i == 1) 
	     {
		   print FILE "                       MISSIONS ";
		   foreach(@{$self->{load_these_missions_only}}) {print FILE sprintf("    M|%1s|","$_");}
		   print FILE "\n";
		 }
	   foreach(@{$self->{PLAUS_ARRAY}->{$m}})  # 1g codes are here in order as in TXT
	     {
	       my $gg4 = $_;         
	       my ($a, $part) = unpack('A1 A3', $gg4);
		   my $segname    = 'XxXxX';  # This segment may not have a NoN-Linear INCRE 
		   next unless(defined $self->{PLAUS}->{$m}->{NL}->{$gg4});
		   
		   for (1 .. 5)  # Incremental Position   
		     {
			   my $incre_position = $_;
		       next unless(defined $self->{PLAUS}->{$m}->{NL}->{$gg4}->{$incre_position});
			   next unless(defined $self->{PLAUS}->{$m}->{NL_SEV_ORD}->{$gg4}->{$incre_position});
			  #my @nl_incre = sort keys %{$self->{PLAUS}->{$m}->{NL}->{$gg4}->{$incre_position}};
			   my @nl_incre = @{$self->{PLAUS}->{$m}->{NL_SEV_ORD}->{$gg4}->{$incre_position}};
			   foreach(@nl_incre)  # 11  , 21,  31, etc   # $self->{PLAUS}->{$mission}->{NL}->{$gg4}->{$incre_position}->{$severity}->{segname}
			     {
				   my $severity = $_;
				   next unless(defined $self->{PLAUS}->{$m}->{NL}->{$gg4}->{$incre_position}->{$severity}->{segname});
				   my $segname  = $self->{PLAUS}->{$m}->{NL}->{$gg4}->{$incre_position}->{$severity}->{segname};
				   my $code     = $self->{PLAUS}->{$m}->{NL}->{$gg4}->{$incre_position}->{$severity}->{nlcode};
		           print FILE sprintf("%-21s |%7s| ","$segname","$code");
				   foreach(@{$self->{load_these_missions_only}})
	                 {
	                    my $mission    = $_;
		                if (defined $self->{PLAUS}->{$mission}->{NL})
		                   {
		                      my $gg4_now = $mission . $part;
			                  my $value   = 'n';
					          if ($segname =~m/XxXxX/) {$value   = 'x';}
				              if (defined $self->{PLAUS}->{$mission}->{NL}->{$gg4_now}->{$incre_position}->{$severity}->{value}) {$value = $self->{PLAUS}->{$mission}->{NL}->{$gg4_now}->{$incre_position}->{$severity}->{value}; print FILE sprintf("%8.2f","$value");} else {print FILE sprintf("%8s","$value");}
                              $self->{PLAUS_TEMP}->{$gg4_now} = 2;
			               }
						else
                          {
                             print FILE sprintf("%8s","x");
                          }	     
	                 } 
                   print FILE "\n";
			     }
	         } # end IncrePosition	
           print FILE "\n";	  
	     } # end ig codes
       $i = 0;			 
      } # end Missions
	  
	print FILE "\n\n\n";	  
	 
	foreach(sort keys %{$self->{PLAUS_TEMP}}) # Checks if we really considered everything #
	  {
	    my $gg4 = $_;
	    unless($self->{PLAUS_TEMP}->{$gg4} == 2) {print FILE "Ignored Segment: |$gg4|\n";}
	  }
	print FILE "\n\n\n";
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


# print STDERR "  *  Generate STH File! Check consistency of *.ana and *.cvt/*.txt files\n";
# print STDERR "  *  Unlimitless FileSize Capabilities & Extreme Detailed Reporting! \n";
# print STDERR "  *  Including: Extreme-Profile-Segment-Block Generator for Plotting! \n";
# print STDERR "  *  Including: AutoRun-without-Stresses & *.stf for 1-D or 2-D Stress Rotation!\n";
# print STDERR "  *  Plausibility Analyses of STF data v6.4 onwards for Pilot Points \n";



1;