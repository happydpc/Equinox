package Tk::COUNT_ANA_STATISTICS_080;

use strict;            # This PacKage Counts Statistics in ANA File! Part of Fatigue Spectra Validation Suite!
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
	$self->{tool_version}  = '8.0';
	
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
	print GLOBAL_LOG "  * |$self->{directory_path}|\n";	
	
	$self->{mission}                  = $root->{MISSION}->{$wmai}->{MISSION};
    $self->{index_file}               = $root->{MISSION}->{$wmai}->{TXT_FILE};
    $self->{flugablauf_file}          = $root->{MISSION}->{$wmai}->{ANA_FILE};	
	
    $self->{count_flight_separate}    = $root->{COUNT_ANA}->{count_flight_separate};       # 1 or 0 [Set to 1 to count flights separately]
    $self->{max_pointsper_flight}     = $root->{COUNT_ANA}->{max_pointsper_flight};        # [only count flights with more peaks than this value - only activated if counting flights separately] 
    $self->{esg_switch}               = $root->{COUNT_ANA}->{esg_switch};                  # 1 or 0
    $self->{cvt_factor_warn}          = $root->{COUNT_ANA}->{cvt_factor_warn};             # [warn if CVT factor higher than this value!]
    $self->{check_round_the_clock}    = $root->{COUNT_ANA}->{check_round_the_clock};       # 1 or 0
    $self->{check_4_return_2_1g}      = $root->{COUNT_ANA}->{check_4_return_2_1g};         # 1 or 0
    $self->{print_special_fsftest}    = $root->{COUNT_ANA}->{print_special_fsftest};       # 1 or 0
    $self->{print_factors_in_log}     = $root->{COUNT_ANA}->{print_factors_in_log};        # 1 or 0
    $self->{run_only_till_flight_num} = $root->{COUNT_ANA}->{run_only_till_flight_num};
    $self->{write_all_1g_sequence}    = $root->{COUNT_ANA}->{write_all_1g_sequence};	

    @{$self->{rtn_codes_provided}}    = split('\s+', $root->{ROT_MLG_CODES}->{ROT});           # e.g. ('1105','4105','7105','2105','5105','8105','3105','6105','9105');  # 4digit Class Code for Rotation
    @{$self->{mlg_codes_provided}}    = split('\s+', $root->{ROT_MLG_CODES}->{MLG});           # e.g ('1133','2138','3144','4133','5138','6144','7133','8138','9144');  # 4digit Class Code for MLG Impact

    print GLOBAL_LOG "\n  * <<START>> Count Statistics ANA & TXT |$wmai|$self->{date}|$self->{index_file}|$self->{flugablauf_file}|\n\n";
    $self->Start_Count_ANA_process($root);
	print GLOBAL_LOG "\n  * <<END>>   Completed Count Statistics Process!\n";
	close(GLOBAL_LOG);
  }


  
 
sub Start_Count_ANA_process
  {
     my $self = shift;
	 my $root = shift;

     $self->{global_start}  = 0;
     foreach (@{$self->{rtn_codes_provided}})   {my $rtc = $_;  $rtc =~s/\s*//g; if ($rtc =~m/\d+/) {push(@{$self->{rotation_codes}}, $rtc);}}
     foreach (@{$self->{mlg_codes_provided}})   {my $rtc = $_;  $rtc =~s/\s*//g; if ($rtc =~m/\d+/) {push(@{$self->{mlg_impact_codes}}, $rtc);}}

     print GLOBAL_LOG "   |$self->{flugablauf_file}|$self->{index_file}|\n";
     unless ((-e $self->{flugablauf_file}) && (-e $self->{index_file})) {print GLOBAL_LOG " *** Not all Input Files Exist!"; exit;}
     $self->{run_only_till_flight_num} = $self->{run_only_till_flight_num} - 2;

     if ($self->{count_flight_separate} > 0)
       {
         $self->{use_max_per_flight} = 1;
         $self->{log_file}           = $self->{flugablauf_file} . '.big.statistics';
         $self->{err_file}           = $self->{flugablauf_file} . '.big.stderr';
     	 $self->{dpp_file}           = $self->{flugablauf_file} . '.big.deltap';	
       }
     else
       { 
         $self->{use_max_per_flight} = 0;
         $self->{log_file}           = $self->{flugablauf_file} . '.all.statistics';
         $self->{err_file}           = $self->{flugablauf_file} . '.all.stderr';
     	 $self->{dpp_file}           = $self->{flugablauf_file} . '.all.deltap';
	     $self->{thm_file}           = $self->{flugablauf_file} . '.all.thermal';
       }

     $self->{dp_max}                   =  0;
     $self->{complete_validity}        =  0;
     $self->{total_points_all_flight}  =  0;
     $self->{txt_cvt_esg_switch}       = 'TXT';
     @{$self->{global_considerations}} = ();
     $self->{switch_off_D2_error}      =  0;
     $self->{mission_ratio_mixed}      = {};

	 $self->{val_file}                 = $self->{flugablauf_file} . '.val.log';
     $self->{ggg_file}                 = $self->{flugablauf_file} . '.ggg.log';

     open(ERR,     ">" . $self->{err_file});
     open(LOG,     ">" . $self->{log_file});
     open(DPP,     ">" . $self->{dpp_file});
	 open(THM,     ">" . $self->{thm_file});
	 open(VAL,     ">" . $self->{val_file});
     open(GGG,     ">" . $self->{ggg_file}) unless ($self->{write_all_1g_sequence} < 1);

     &load_loadcase_data_into_structure_count_only($self);
     &load_one_FT_Block_from_flugabluaf_and_process_count_only($self);

     if (($self->{use_max_per_flight} >  0) && ($self->{count_flight_separate} == 0))
       {
         &write_summary_statistics_only_BIG_flights($self);
         #&write_summary_dp_and_RClock_findings($self);   # not checked yet!
       }

     if ($self->{use_max_per_flight} == 0)
       {
         $self->{average_points_per_flight} = $self->{total_points_all_flight} / $self->{complete_validity};
         &write_summary_statistics_all_flights($self);
     	 &check_order_of_flight_types_or_missing_flights_in_ana($self);
         &write_summary_dp_and_RClock_and_TXT_findings($self);
       }
     close(ERR);
     close(LOG);
     close(DPP);
	 close(THM);
	 close(VAL);
     close(GGG) unless ($self->{write_all_1g_sequence} < 1);
     #print STDERR "  * end of process!";

  } # end sub




sub check_order_of_flight_types_or_missing_flights_in_ana
  {
    my $self     = shift;
	my $i        = 0;
	my $maxftnum = 0;
	foreach (@{$self->{order_of_flights_in_ana}})
	  {
	    $_ =~m/(\d+)\_/;
	    my $f_no = $1;
		$i++;
		unless ($f_no == $i)  {print LOG sprintf("\n *** ERROR WroNG OrDER of Flight_Types! Expected to see TF_[%-4s] but found TF_[%-4s] !!!!!%s","$i","$f_no","\n");}
        $self->{these_flights_really_seen}->{$f_no} = 1;	if ($f_no > $maxftnum) {$maxftnum = $f_no;}
	  }
	unless ($maxftnum == $self->{total_flight_types}) {print LOG sprintf("\n *** ERROR Unexpected Number of Flight Types! Expected to see [%-3s] Flight_Types BUT found [%-3s] Flight_Types!!!!!%s","$maxftnum","$self->{total_flight_types}","\n");}

	for (1 .. $self->{total_flight_types}) 
	  { 
	    my $n = $_;
	    unless (defined $self->{these_flights_really_seen}->{$n}) {print LOG sprintf("\n *** ERROR MISSING FLIGHT TYPE!!!!! Where is TF_[%-4s] ????? %s","$n","\n");}
	  }
	  
	for (1 .. $self->{total_flight_types}) 
	  { 
	    my $n = $_;
	    unless ($self->{mlg_is_in_this_flight}->{$n} > 0) {print LOG sprintf("\n  ** WARNING missing MLG incre in this FLIGHT TYPE!!!!! Where is MLG in TF_[%-4s] ????? %s","$n","\n");}
	  }	 
	for (1 .. $self->{total_flight_types}) 
	  { 
	    my $n = $_;
	    if ($self->{nlg_is_in_this_flight}->{$n} > 0) {print LOG sprintf("\n  *  INFO found NLG incre in this FLIGHT TYPE TF_[%-4s] %s","$n","\n");}
	  }	  
  }


sub write_summary_dp_and_RClock_and_TXT_findings
  {
    my $self   = shift;   
    &check_the_max_dp_reached_per_flight($self);
    &write_out_max_dp_values_in_all_steady_1g_codes($self); 
	&check_how_many_times_the_same_loadcase_code_occurs_in_the_Mission($self);
	&check_if_same_ISSY_number_used_for_MORE_than_one_Event($self);
	&check_if_the_segment_number_of_INCRE_is_same_as_the_STEADY_segment_number($self);
	&warn_when_expected_combinations_do_not_exist($self);

    my $i = 0;	
    if (defined @{$self->{CODES_NOT_USED}}[0])
	  {
	    print LOG "\n\n *** WARN  - These CLASS CODEs & ISSYNUMs are not used in ANA:\n";
        foreach (@{$self->{CODES_NOT_USED}}) {print LOG sprintf("%9s","$_"); $i++; if ($i > 9) {print LOG "\n"; $i = 0;};}
		print LOG "\n";
		foreach (@{$self->{ISSY_NOT_USED}})  {print LOG sprintf("%9s","$_"); $i++; if ($i > 9) {print LOG "\n"; $i = 0;};}
	  }	
    $i = 0;
    if (defined @{$self->{LCASE_TXT_CVT_WARN}}[0])
	  {
	    print LOG "\n\n *** ERROR - LOADCASE TXT/CVT global checks for FACTORS & ISSYNUM & CLASS_CODES: (Please Investigate these with ConvTable Information!):\n";
        foreach (@{$self->{LCASE_TXT_CVT_WARN}}) {print LOG "   $_"; $i++; if ($i > 0) {print LOG "\n"; $i = 0;};}
	  }
    $i = 0;	  
    if (defined @{$self->{LCASE_TXT_CVT_WARN_EXTRA}}[0])
	  {
	    print LOG "\n\n  ** WARNING - additional checks Specific for VG Non-Symmetry: (Usually Only VG segments expected to be Un-Symmetric!):\n";
        foreach (@{$self->{LCASE_TXT_CVT_WARN_EXTRA}}) {print LOG "   $_"; $i++; if ($i > 0) {print LOG "\n"; $i = 0;};}
	  }	
    $i = 0;	  
    if (defined @{$self->{GG_SEGMENT_ORDER_WARN}}[0])
	  {
	    print LOG "\n\n  ** WARNING - The 1g segments in these Flights do not follow the Segment ORDER defined in CVT/TXT!):\n";
        foreach (@{$self->{GG_SEGMENT_ORDER_WARN}}) {print LOG "   $_"; $i++; if ($i > 0) {print LOG "\n"; $i = 0;};}
	  }
    $i = 0;
    if (defined @{$self->{back2_1g_considerations}}[0])
	  {
	    my @a = @{$self->{back2_1g_considerations}};     my $n = $#a + 1;     my $t = $self->{total_flight_types};	  
	    print LOG "\n\n ** WARNING - following Flights have <RETURN to 1g> for Rotation OR MLG Impact (Check if this is Allowed for this SPECTRA) [Occurs $n in $t Flights]:\n";
        foreach (@{$self->{back2_1g_considerations}}) {print LOG " $_"; $i++; if ($i > 4) {print LOG "\n"; $i = 0;};}
	  }	
	$i = 0;	
    if (defined @{$self->{DP_JUMP_WARN}}[0])
	  {
	    my @a = @{$self->{DP_JUMP_WARN}};     my $n = $#a + 1;     my $t = $self->{total_flight_types};
	    print LOG "\n\n ** WARNING - following Flights have a JUMP in DeltaPressure Values [$n / $t Flights Affected]:\n";
        foreach (@{$self->{DP_JUMP_WARN}}) {print LOG " $_"; $i++; if ($i > 4) {print LOG "\n"; $i = 0;};}
	  }
    $i = 0;	  
    if (defined @{$self->{DP_LOW_WARN}}[0])
	  {
	    my @a = @{$self->{DP_LOW_WARN}};     my $n = $#a + 1;     my $t = $self->{total_flight_types};	    
	    print LOG "\n\n ** WARNING - following Flights did not reach MAX DeltaPressure [$n / $t Flights Affected]:\n";
        foreach (@{$self->{DP_LOW_WARN}}) {print LOG " $_"; $i++; if ($i > 4) {print LOG "\n"; $i = 0;};}
	  }
    $i = 0;
    if (defined @{$self->{ROUND_CLOCK_WARN_B}}[0])
	  {
	    print LOG "\n\n ** WARNING - Details of Sequence of 1st Incre Position (i.e 1st out of 5) that did not meet defined criteria!\n";
		print LOG "            - Up/Down Peaks of 1st Incre Position may not Follow Flow-Sequence!\n";
        foreach (@{$self->{ROUND_CLOCK_WARN_B}}) {print LOG " $_"; $i++; if ($i > 1) {print LOG "\n"; $i = 0;};}
	  }
    $i = 0;	  
    if (defined @{$self->{ROUND_CLOCK_WARN_A}}[0])
	  {
	    print LOG "\n\n ** WARNING - Details of Sequence of 1st Incre Position (i.e 1st out of 5) that did not meet defined criteria!\n";
		print LOG "            - Up/Down Peaks of 1st Incre Position may not Follow Flow-Sequence!\n";
        foreach (@{$self->{ROUND_CLOCK_WARN_A}}) {print LOG " $_"; $i++; if ($i > 1) {print LOG "\n"; $i = 0;};}
	  }	
    $i = 0;		  
	unless ($self->{write_all_1g_sequence} < 1)
	  {
	    my @matrix = @{$self->{matrix_all_1g_segments}->{1}};
		my @fnames = @{$self->{order_of_flights_in_ana}};
		for (1 .. $self->{total_flight_types}) {my $n = $_; my $fname = shift(@fnames); print GGG sprintf("%-4s %-20s","$n","$fname");}
		print GGG "\n";
	    for (0 .. $#matrix)
	       {
		     my $seg_row = $_;  # number of 1g segments in the flight!
	         for (1 .. $self->{total_flight_types}) 
			   {
			     my $segname   = 'WARN';
				 if (defined ${$self->{matrix_all_1g_segments}->{$_}}[$seg_row]) {$segname =  ${$self->{matrix_all_1g_segments}->{$_}}[$seg_row] ;}
				 print GGG sprintf("%-25s","$segname");
			   }
			 print GGG "\n";  
		   }
       }
  }
  

sub check_the_max_dp_reached_per_flight
  {
    my $self   = shift; 
	foreach (keys %{$self->{max_deltap_vl}})
	  {
	    my $value = $self->{max_deltap_vl}->{$_};
		my $mn    = $self->{max_deltap_mn}->{$_};
		if ($value < $self->{dp_max}) 
		{
		  my $header = $self->{max_deltap_hd}->{$_};
		  push(@{$self->{DP_LOW_WARN}}, sprintf("[DP:%6.2f] [M:%2s] %-30s ","$value","$mn","$header"));
		  #print LOG " *** Warning: Max DP not reached $header\n";
		}
	  }	   
  }


  
sub check_if_segments_sequence_is_same_as_defined_in_TXT
  {
    my $self   = shift;
	my $num    = shift;
	my $mn     = shift;
	my $header = shift;
	my $warn   = 0;   #print LOG " :::::::::::  Flight $num  Mission $mn  $header :::::::::::: \n";
	my @txt_1g_order    = @{$self->{ALLCODES}->{GG_ORDER}->{$mn}};
	my @flight_1g_order = @{$self->{GG_ORDER_THIS_FLIGHT}};

    my $lf =	$flight_1g_order[$#flight_1g_order] . '0';
    my $lt =	$txt_1g_order[$#txt_1g_order];		
	unless ($lf == $lt) {$warn = 2;}
	my $k = $#flight_1g_order + 1;
	my $i = 0;	
	my $j = 0;
	$self->{seen_already} = {};
	unless ($warn > 0)
	 {
		for (0 .. 1000) # It is assummed that we have MORE segments in TXT - if not all segments are used!
		  {
		    last unless (defined $txt_1g_order[$_]);
		    my $txt = $txt_1g_order[$_];
			my $flt = $flight_1g_order[$i]; $flt = $flt . '0'; 
			if (defined $self->{seen_already}->{$flt}) {$warn = 4; last;}
			if ($i == 0) {unless ($txt == $flt) {$warn = 1; last;}}		
			$self->{seen_already}->{$txt} = 1;			#print LOG " $txt  |  $flt \n";
            if ($txt == $flt) {$i++; $j++;  next;}
		  }
        unless ($j == $k) {$warn = 3;}		  
	  }

	if ($warn == 1) {push(@{$self->{GG_SEGMENT_ORDER_WARN}}, sprintf(" *** ERROR   Flight_Num: %5i [M:%2s] %-21s ---> Segments in Flight Missing OR Not Correctly Ordered! First Code!","$num","$mn","$header"));}
	if ($warn == 2) {push(@{$self->{GG_SEGMENT_ORDER_WARN}}, sprintf(" *** ERROR   Flight_Num: %5i [M:%2s] %-21s ---> Segments in Flight Missing OR Not Correctly Ordered! Last  Code!","$num","$mn","$header"));}
	if ($warn == 3) {push(@{$self->{GG_SEGMENT_ORDER_WARN}}, sprintf(" *** ERROR   Flight_Num: %5i [M:%2s] %-21s ---> Segments in Flight Missing OR Not Correctly Ordered! Not All There!","$num","$mn","$header"));}
	if ($warn == 4) {push(@{$self->{GG_SEGMENT_ORDER_WARN}}, sprintf(" *** ERROR   Flight_Num: %5i [M:%2s] %-21s ---> This Segment seen at least 2-wice in Flight Mission! What Happened!","$num","$mn","$header"));}

	unless ($self->{write_all_1g_sequence} < 1)
	 { #print GGG "\nTF $num ###############################################\n";
	    my @txt_1g_orderA    = @{$self->{ALLCODES}->{GG_ORDER}->{$mn}};
		$self->{compressed_1g_flt_sequence} = {};
		my $prev = '123456789';
		my $i    = -1;	
		
		foreach(@{$self->{GG_ORDER_THIS_FLIGHT}})
			{
			  if ($prev != $_) {$i = $i + 1;}		
			  $self->{compressed_1g_flt_sequence}->{$i} = $_;
			  $prev = $_;
			}
			
		my $txt_segname = 'T';
		my $flt_segname = 'F';
        my $j  = 0;
		for (0 .. 1000) # It is assummed that we have MORE segments in TXT - if not all segments are used!
		  {
		    last unless (defined $txt_1g_orderA[$_]);
		    my $txt = $txt_1g_orderA[$_]; 
			my $flt = $self->{compressed_1g_flt_sequence}->{$j} . '0';
		    $j++;
			if (defined $self->{LCASE}->{$txt}->{segname}->{1}) {$txt_segname = $self->{LCASE}->{$txt}->{segname}->{1};}	
			if (defined $self->{LCASE}->{$flt}->{segname}->{1}) {$flt_segname = $self->{LCASE}->{$flt}->{segname}->{1};}
			#print GGG "$txt  $txt_segname | $flt  $flt_segname \n";
            if ($txt != $flt) 
			  {
			     push(@{$self->{matrix_all_1g_segments}->{$num}}, 'NA'); #print GGG "$txt  $txt_segname | $flt  NA \n";
				 $j--; next;
			  }
			if ($txt == $flt) 
			  {
			    push(@{$self->{matrix_all_1g_segments}->{$num}}, $flt_segname); # print GGG "$txt  $txt_segname | $flt  $flt_segname \n";
			  }
		  }	  
	  }
	} # end sub


  
sub check_the_DeltaPressure_profile
  {
    my $self   = shift;
	my $num    = shift;
	my $mn     = shift;
	my $header = shift;
	my $updown = 0; my $jva = 0; my $jvb = 0;
	my $a      = 0;
	my $warn   = 0;
	$self->{max_deltap_vl}->{$num}  = 0;
	$self->{max_deltap_hd}->{$num}  = $header;
	$self->{max_deltap_mn}->{$num}  = $mn;
	
	unless (defined $self->{max_dp_per_mission}->{$mn}) {$self->{max_dp_per_mission}->{$mn} = 0;}
	
	foreach (@{$self->{DP_TF_ARRAY}})
	{
	  if    (($_ < $a) && ($updown < 1))  {$updown = 1;}
	  elsif (($_ < $a) && ($updown == 1)) {$updown = 2;}
	  
	  if (($_ > $a) && ($updown == 1)) {$warn = 1; $jva = $_ - $a;}
	  if (($_ > $a) && ($updown > 1))  {$warn = 1; $jvb = $_ - $a;}
	  $a = $_;
	  
	  if ($_ > $self->{max_deltap_vl}->{$num})     {$self->{max_deltap_vl}->{$num}     = $_;}
	  if ($_ > $self->{max_dp_per_mission}->{$mn}) {$self->{max_dp_per_mission}->{$mn} = $_;}	  
	}
	
	if ($warn > 0) {push(@{$self->{DP_JUMP_WARN}}, sprintf("[%04s] [%02s][%02s] %-30s","$num","$jva","$jvb","$header"));}
	print DPP "FLIGHT  $num  $header \n";  foreach (@{$self->{DP_TF_ARRAY}}) {print DPP "$_\n";}
	print THM "FLIGHT  $num  $header \n";  foreach (@{$self->{TH_TF_ARRAY}}) {print THM "$_\n";}
  }

  
sub write_out_max_dp_values_in_all_steady_1g_codes
  {
    my $self      = shift;
	
    print LOG "\n\n\n_________________ ComBiNaTionS of CoDeS _____________________________________________________________________________         Event A                   Total    Combi  UnCombi   Event B                   Total  UnCombi\n";
    foreach (sort keys %{$self->{statistics_combi}})       
      {
         my $key    = $_;
         foreach (sort keys %{$self->{statistics_combi}->{$key}}) # $self->{statistics_combi}->{$incre}->{$a}
           {
             my $value  = $_;
             my $koccur = $self->{statistics_combi_count}->{$key}->{$value}; 
             my $kall   = $self->{total_occur_per_code}->{$key};
             my $vall   = $self->{total_occur_per_code}->{$value};
             my $name_a = 'A'; my $name_b = 'B'; 
             if (defined $self->{LCASE}->{$key}->{segname}->{1})   {$name_a = $self->{LCASE}->{$key}->{segname}->{1};}
             if (defined $self->{LCASE}->{$value}->{segname}->{1}) {$name_b = $self->{LCASE}->{$value}->{segname}->{1};}
             
             unless ($value eq 'A')
               {
                  my $free_key   = $kall - $koccur;       #EXC_LG-09_F1   1488   16    EXC_VG-08_F1    1488    16 
                  my $free_value = $vall - $koccur; 
                  print LOG sprintf("%-22s %-8s [%6s/%-8s]   %-18s %-22s %-8s [ALL:%8s] %8s","$name_a","$key","$koccur","$kall","|<Is_CoMbiNeD>|","$name_b","$value","$vall"," ");
                  print LOG sprintf("%-22s %8s %8s %8s   %-22s %8s %8s%s",                   "$name_a","$kall","$koccur","$free_key","$name_b","$vall","$free_value","\n");
               }
             #print LOG sprintf("%-22s %-8s [%6s/%-8s]   %-18s %-22s %-8s [ALL:%8s]%s","$name_a","$key","$koccur","$kall","|<Is_CoMbiNeD>|","$name_b","$value","$vall","\n") unless ($value eq 'A');
          }
      }
    print LOG "__________________________________________________________________________________________________________________________________________________________________________________________________________________________\n\n\n";	
    print LOG "#++++++++++++ Max DP per Steady 1g Coco TXT/CVT Ordered ++++++++++++++++#\n";  # The 1g codes Sequence is the same as in TXT file!
    print LOG "#++++++++ CODE ++++++ DP_max [ +++++ MIN ==>> ++++++ MAX] ++++++++++++++#\n";
    
    #foreach (sort keys %{$self->{max_dp_per_steady_1g}})
	foreach (@{$self->{exact_order_of_gg_seen_in_txt_for_flight}})
      {
		my $gg_code  = $_;
		unless (defined $self->{max_dp_per_steady_1g}->{$gg_code}) {print LOG " ** undefined 1g Code |$gg_code|\n"; next;};
		my $dp_v_max = $self->{max_dp_per_steady_1g}->{$gg_code};
		my $dp_v_min = $self->{min_dp_per_steady_1g}->{$gg_code};
		my $diff     = abs($dp_v_max - $dp_v_min);
		if ($diff > 0.1) {print LOG sprintf("%15s %10.2f   [%10.2f ==>> %10.2f] %20s%s","$gg_code","$dp_v_max","$dp_v_min","$dp_v_max","*** ERROR!","\n");}
		else             {print LOG sprintf("%15s %10.2f   [%10.2f ==>> %10.2f]%s","$gg_code","$dp_v_max","$dp_v_min","$dp_v_max","\n");}
      }
    print LOG "#+++++++++++++++++++++++++++++++++++++++++++++++++++++++#\n\n";
  }

sub include_missions_ratio_in_mix_mission
   {
     my $self      = shift; 
	 my $t         = $self->{complete_validity};            
	 foreach(sort keys %{$self->{mission_ratio_mixed}})
	    {
		  my $mn    = $_;
		  my $val   = $self->{mission_ratio_mixed}->{$mn}; 
		  my $ratio = $val / $t;
		  my $max_dp_this_mission = $self->{max_dp_per_mission}->{$mn};
		  print LOG sprintf("  * Mission [M%s] [%5s / %5s] Ratio:%5.4f   MaxDP: %5.2f%s","$mn","$val","$t","$ratio","$max_dp_this_mission","\n");
		}
   }


sub check_how_many_flights_we_see_this_event_in_all_flight_types   
  {
    my $self      = shift;
    my $code      = shift;
    my $event     = shift;
    my $d         = shift;	
	my $valid     = 0;
	my $i         = 0;
   
    if ($event =~m/1g/)
	  {
	    my @keys = keys %{$self->{flights_validity_seen}->{$code}};       # get the fnums i.e. $self->{flights_validity_seen}->{$gg_code}->{$fnum}
		foreach (@keys)
		  {
		    $i++;
		    $valid = $valid + $self->{flights_validity_seen}->{$code}->{$_};
		  }
	  }
   elsif($event =~m/incre/)
     {
	    my @keys = keys %{$self->{flights_validity_seen}->{$d}->{$code}}; # get the fnums i.e. $self->{flights_validity_seen}->{$d}->{$incre}->{$fnum}
		foreach (@keys)
		  {
		    $i++;
		    $valid = $valid + $self->{flights_validity_seen}->{$d}->{$code}->{$_};
		  }
	 }
	  
	$self->{SEEN}->{F}  =  $i;
	$self->{SEEN}->{V}  = $valid;   
  }
   
   
   
sub write_summary_statistics_all_flights
  {
    my $self      = shift;
    my $prev      = 00000;
    my $appf      = sprintf("%10.2f","$self->{average_points_per_flight}");
	@{$self->{exact_order_of_gg_seen_in_txt_for_flight}} = ();
    print LOG "______________ version $self->{tool_version} |$self->{date}| ___________________\n";	
    print LOG "_________You are using file Format: [$self->{txt_cvt_esg_switch}]__________________\n";
    print LOG "____________ [Total_Flight_Types: $self->{total_flight_types}] ____________________\n";
    print LOG "  * Deltap Max.value: $self->{dp_max}         \n";
    print LOG "  * Spectra Validity: $self->{complete_validity}\n";
    print LOG "  * Average Points / Flight: $appf\n";	
	&include_missions_ratio_in_mix_mission($self);
    print LOG "______________________ statistics _________________________\n";
	print VAL "\n\n# Example: [occurs in:    4 TFs M2] / [all_mission_TFs:  317]    [TFs_validity:    6] / [total_validity:  400]    !!!!!  ";
	print VAL "# This event is seen in Only 4 Flight Types which has a Total Validity of 6!   # The Mission M2 has 317 Flight Types VALID for 400 Flights!\n\n\n";
	
    foreach (@{$self->{global_considerations}})   {print LOG "$_ \n";}
	
    foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
	my $code    = $_;
	my $length  = length($code);
	my $i       = 0;
	my $j       = 0;
	my $mission = unpack('A1 A4', $code);
	my @ftypes_mission = keys %{$self->{this_mission_has_thes_number_of_ftypes}->{$mission}};
	my $n_ft_m  = $#ftypes_mission + 1;
	$self->{SEEN}->{F}  = 0;  # this event seen in these number of FTypes!
	$self->{SEEN}->{V}  = 0;  # this event seen in these number of FTypes has this Validity!
	
	if ($prev == $code)   {next;}
	if ($code  =~m/000$/) {my $issynum = $self->{LCASE}->{$code}->{issyno}->{1}; print LOG sprintf("%-24s%-9s%-5s%6s%11s%s","DPCASE","$code","$issynum","dp","N/A","\n"); print VAL sprintf("%-24s%-9s%-5s%6s%11s%s","DPCASE","$code","$issynum","dp","N/A","\n");next;}
	$self->{total_occur_per_code}->{$code} = 0;
                
    my $gg_alone = 0;
    if (defined $self->{gg_codes_alone}->{$code}) 
       {
         $gg_alone = $self->{gg_codes_alone}->{$code};
       }
				
	if (defined $self->{statistics}->{GG}->{$code})
	  {
	    push(@{$self->{exact_order_of_gg_seen_in_txt_for_flight}}, $code);
	    my $occur   = $self->{statistics}->{GG}->{$code};
	    my $segname = $self->{LCASE}->{$code}->{segname}->{1}; 
		my $issynum = $self->{LCASE}->{$code}->{issyno}->{1};
		if ($gg_alone > 0)
		  {
		    my $mn       = $self->{LCASE}->{$code}->{mission}->{1};
		    my $gg_ratio = $gg_alone / $self->{mission_ratio_mixed}->{$mn};
	        print LOG sprintf("%-24s%-9s%-5s%6s%11i [%5i UnComb] [%3.2f/UnCFT]%s","$segname","$code","$issynum","steady","$occur","$gg_alone","$gg_ratio","\n") ;
		  }
	    print LOG sprintf("%-24s%-9s%-5s%6s%11i [%5i UnComb] WARN! WHY?%s","$segname","$code","$issynum","steady","$occur","$gg_alone","\n") if ($gg_alone == 0);		
        print LOG sprintf("%-24s%-9s%-5s%6s%11i%s","$segname","$code","$issynum","steadyA","$gg_alone","\n") unless ($self->{print_special_fsftest} < 1);
		&check_how_many_flights_we_see_this_event_in_all_flight_types($self, $code, '1g', '0');  # $self->{flights_validity_seen}->{$gg_code}->{$fnum} = $valid;
	    print VAL sprintf("%-24s%-9s%-5s %5s    [occurs in:%5s TFs M%s] / [all_mission_TFs:%5s]    [TFs_validity:%5s] / [total_validity:%5s]%s","$segname","$code","$issynum"," ","$self->{SEEN}->{F}","$mission","$n_ft_m","$self->{SEEN}->{V}","$self->{mission_ratio_mixed}->{$mission}","\n");			
	    next;
	  }

	for (1 .. 9) 
	  {
	    my $d       = $_;
	    my $message = '';
	    if (defined $self->{statistics}->{$d}->{$code})
	      {
		$i++;
		my $occur   = $self->{statistics}->{$d}->{$code};
		my $segname = $self->{LCASE}->{$code}->{segname}->{$d};
		my $issynum = $self->{LCASE}->{$code}->{issyno}->{$d};
		if ($length > 5) {$segname = $self->{LCASE}->{$code}->{segname}->{1}; $issynum = $self->{LCASE}->{$code}->{issyno}->{1};}  # NL Cases!	
		if (defined $segname)
		  {
		    print LOG sprintf("%-24s%-9s%-5s%6s%11i","$segname","$code","$issynum","neg","$occur") if ($d =~m/2/);               # ($d =~m/2|4|6|8/);
		    print LOG sprintf("%-24s%-9s%-5s%6s%11i","$segname","$code","$issynum","pos","$occur") if ($d =~m/1|3|4|5|6|7|8|9/); # ($d =~m/1|3|5|7|9/);
			
		    &check_how_many_flights_we_see_this_event_in_all_flight_types($self, $code, 'incre', $d);    # $self->{flights_validity_seen}->{$d}->{$incre}->{$fnum} = $valid;		
	        print VAL sprintf("%-24s%-9s%-5s %5s    [occurs in:%5s TFs M%s] / [all_mission_TFs:%5s]    [TFs_validity:%5s] / [total_validity:%5s]","$segname","$code","$issynum"," ","$self->{SEEN}->{F}","$mission","$n_ft_m","$self->{SEEN}->{V}","$self->{mission_ratio_mixed}->{$mission}");	
            if ($self->{SEEN}->{V} < ($self->{mission_ratio_mixed}->{$mission} * 0.1)) {print VAL "    !!!!!\n"} else {print VAL "\n"}
          }
		else
		  {
		    print LOG sprintf("%-24s%-9s|D:%2s|%11i  *** ERROR ","UndeFinTXT_eXisTinANA","$code","$d","$occur");
			print VAL sprintf("%-24s%-9s|D:%2s|%11i  *** ERROR ","UndeFinTXT_eXisTinANA","$code","$d","$occur");
		  }
		  
		$self->{total_occur_per_code}->{$code} = $self->{total_occur_per_code}->{$code} + $occur;
		
		for (0 .. 8)
		  {
		    if (defined $self->{stats_more}->{$d}->{$_}->{$code})                                    
		      {
			     my $occur_i = $self->{stats_more}->{$d}->{$_}->{$code};
                 my $fac_i   = @{$self->{LCASE}->{$code}->{fac}->{$d}}[$_]; # Check if we have cycles but the factor is zero?
                 if (($occur_i > 0) && (abs($fac_i) < 0.00001)) {$message = "  *** ERROR: Occurences found But Factor is Zero!";}
			     print LOG sprintf("%8i","$occur_i");
				 if (defined $self->{stats_more}->{$d-1}->{$_}->{$code})  # check_if_the_Occurences_for_the_event_are_symmetrical_for_all_directions (only catches LINEAR CASES ONLY!)
				   {
				     my $occur_i_prev = $self->{stats_more}->{$d-1}->{$_}->{$code};
				     unless ($occur_i == $occur_i_prev) {$message = $message . "  *** ERROR: Occurences are NOT symmetrical for all directions!";}
				   }
		      } else {print LOG sprintf("%8s","");}
		  }
		print LOG "# $message #";		  
        if ($self->{print_factors_in_log} > 0) {if (defined @{$self->{LCASE}->{$code}->{fac}->{$d}}[0]) {my @f_now  = @{$self->{LCASE}->{$code}->{fac}->{$d}}; foreach(@f_now) {print LOG sprintf("%8s","$_") unless ($_ =~m/-0-/);}} elsif ($length > 5) {my $fac = @{$self->{LCASE}->{$code}->{fac}->{1}}[0]; print LOG sprintf("%8s","$fac") unless ($fac =~m/-0-/);}}
		print LOG "\n";
	      }
       elsif((defined $self->{LCASE}->{$code}->{segname}->{$d}) && ($length == 5))
          {
		    $j++;
            my $segname = $self->{LCASE}->{$code}->{segname}->{$d};
	        my $issynum = $self->{LCASE}->{$code}->{issyno}->{$d};
	        if ($length > 5) {$segname = $self->{LCASE}->{$code}->{segname}->{1}; $issynum = $self->{LCASE}->{$code}->{issyno}->{1};}       # NL Cases!	
	        print LOG sprintf("%-24s%-9s%-5s%6s%25s","$segname","$code","$issynum","neg","<no-statistics>") if ($d =~m/2/);               # ($d =~m/2|4|6|8/);
	        print LOG sprintf("%-24s%-9s%-5s%6s%25s","$segname","$code","$issynum","pos","<no-statistics>") if ($d =~m/1|3|4|5|6|7|8|9/); # ($d =~m/1|3|5|7|9/);
            if ($self->{print_factors_in_log} > 0) {if (defined @{$self->{LCASE}->{$code}->{fac}->{$d}}[0]) {my @f_now  = @{$self->{LCASE}->{$code}->{fac}->{$d}}; foreach(@f_now) {print LOG sprintf("%8s","$_") unless ($_ =~m/-0-/);}} elsif ($length > 5) {my $fac = @{$self->{LCASE}->{$code}->{fac}->{1}}[0]; print LOG sprintf("%8s","$fac") unless ($fac =~m/-0-/);}}
		    print LOG "\n";
			push(@{$self->{CODES_NOT_USED}}, $code); push(@{$self->{ISSY_NOT_USED}}, $issynum);
			
		    &check_how_many_flights_we_see_this_event_in_all_flight_types($self, $code, 'incre', $d);    # $self->{flights_validity_seen}->{$d}->{$incre}->{$fnum} = $valid;
	        print VAL sprintf("%-24s%-9s%-5s %5s    [occurs in:%5s TFs M%s] / [all_mission_TFs:%5s]    [TFs_validity:%5s] / [total_validity:%5s]","$segname","$code","$issynum"," ","$self->{SEEN}->{F}","$mission","$n_ft_m","$self->{SEEN}->{V}","$self->{mission_ratio_mixed}->{$mission}");	
            if ($self->{SEEN}->{V} < ($self->{mission_ratio_mixed}->{$mission} * 0.1)) {print VAL "    !!!!!\n"} else {print VAL "\n"}
		  }
	  }

	if (($i == 0) && ($j == 0))
          {
	        my $segname = $self->{LCASE}->{$code}->{segname}->{1};
	        my $issynum = $self->{LCASE}->{$code}->{issyno}->{1};
	        if(defined $segname) 
			   {print LOG sprintf("%-24s%-9s%-5s%6s%11s%30s%s","$segname","$code","$issynum","N/A","N/A","** WARN Event_not_Used","\n");  print VAL sprintf("%-24s%-9s%-5s%6s%11s%30s%s","$segname","$code","$issynum","N/A"," "," ","\n");}
	        else
               {print LOG sprintf("%-24s%-9s%-5s%6s%11s%s","*** ERROR ***","$code","$issynum","N/A","N/A","\n");                          print VAL sprintf("%-24s%-9s%-5s%6s%11s%s","*** ERROR ***","$code","$issynum","N/A","N/A","\n");}
			push(@{$self->{CODES_NOT_USED}}, $code); push(@{$self->{ISSY_NOT_USED}}, $issynum);
          }	
        $prev = $code;
      }
    print LOG "_________________ statistics _________________   \n";
    #print LOG "  * deltap max.value: $self->{dp_max}            \n";
    #print LOG "  * spectra validity: $self->{complete_validity} \n";
  }


  

sub write_summary_statistics_only_BIG_flights
  {
    my $self      = shift;
    my $prev      = 00000;
	my $appf      = $self->{BLOCK}->{POINTS} / $self->{BLOCK}->{VALID};  $appf = sprintf("%10.2f","$appf");
    print LOG "______________________________________________\n";
    print LOG "Counted Only Flights with Points higher than: $self->{max_pointsper_flight}\n";
    print LOG "  * Deltap max.value: $self->{dp_max}         \n";
    print LOG "  * Peaks: $self->{BLOCK}->{POINTS}   Spectra Validity: $self->{complete_validity}\n";
    print LOG "  * Average Points/Flight: $appf [only for info for this flight!]\n";
    print LOG "_________________ statistics _________________\n";
	
    foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
	my $code   = $_;
	my $length = length($code);
	my $i      = 0;		
	if ($prev == $code)   {next;}
	if ($code  =~m/000$/) {my $issynum = $self->{LCASE}->{$code}->{issyno}->{1}; print LOG sprintf("%-24s%-9s%-5s%6s%11s%s","DPCASE","$code","$issynum","dp","N/A","\n"); next;}
	$self->{total_occur_per_code}->{$code} = 0;
                
    my $gg_alone = 0;
    if (defined $self->{gg_codes_alone}->{$code}) 
       {
         $gg_alone = $self->{gg_codes_alone}->{$code};
       }
				
	if (defined $self->{statistics}->{GG}->{$code})
	  {
	    my $occur   = $self->{statistics}->{GG}->{$code};
	    my $segname = $self->{LCASE}->{$code}->{segname}->{1}; 
		my $issynum = $self->{LCASE}->{$code}->{issyno}->{1};
		if ($gg_alone > 0)
		  {
		    my $mn       = $self->{LCASE}->{$code}->{mission}->{1};
		    my $gg_ratio = $gg_alone ;
	        print LOG sprintf("%-24s%-9s%-5s%6s%11i [%5i UnComb] [%3.2f/UnCFT]%s","$segname","$code","$issynum","steady","$occur","$gg_alone","$gg_ratio","\n") ;
		  }
	    print LOG sprintf("%-24s%-9s%-5s%6s%11i [%5i UnComb] WARN! WHY?%s","$segname","$code","$issynum","steady","$occur","$gg_alone","\n") if ($gg_alone == 0);		
        print LOG sprintf("%-24s%-9s%-5s%6s%11i%s","$segname","$code","$issynum","steadyA","$gg_alone","\n") unless ($self->{print_special_fsftest} < 1);
	    next;
	  }
###########	  
	for (1 .. 9) 
	  {
	    my $d = $_;
	
	    if (defined $self->{statistics}->{$d}->{$code})
	      {
		$i++;
		my $occur   = $self->{statistics}->{$d}->{$code};
		my $segname = $self->{LCASE}->{$code}->{segname}->{$d};
		my $issynum = $self->{LCASE}->{$code}->{issyno}->{$d};
		if ($length > 5) {$segname = $self->{LCASE}->{$code}->{segname}->{1}; $issynum = $self->{LCASE}->{$code}->{issyno}->{1};}  # NL Cases!	
		if (defined $segname)
		  {
		    print LOG sprintf("%-24s%-9s%-5s%6s%11i","$segname","$code","$issynum","neg","$occur") if ($d =~m/2/);               # ($d =~m/2|4|6|8/);
		    print LOG sprintf("%-24s%-9s%-5s%6s%11i","$segname","$code","$issynum","pos","$occur") if ($d =~m/1|3|4|5|6|7|8|9/); # ($d =~m/1|3|5|7|9/);
		  }
		else
		  {
		    print LOG sprintf("%-24s%-9s|D:%2s|%11i","UndeFinTXT_eXisTinANA","$code","$d","$occur");
		  }
		  
		$self->{total_occur_per_code}->{$code} = $self->{total_occur_per_code}->{$code} + $occur;
		
		for (0 .. 8)
		  {
		    if (defined $self->{stats_more}->{$d}->{$_}->{$code})
		      {
			my $occur_i = $self->{stats_more}->{$d}->{$_}->{$code};
			print LOG sprintf("%8i","$occur_i");
		      } else {print LOG sprintf("%8s","");}
		  }
		print LOG "\n";			
	      }
	  }

	unless ($i > 0)
          {
	    my $segname = $self->{LCASE}->{$code}->{segname}->{1};
		my $issynum = $self->{LCASE}->{$code}->{issyno}->{1};
	    if (defined $segname) 
	      {print LOG sprintf("%-24s%-9s%-5s%s","$segname","$code","$issynum","\n");}
	    else 
	      {print LOG sprintf("%-24s%-9s%-5s%6s%11s%s","*** ERROR ***","$code","$issynum","N/A","N/A","\n");}
          }	
        $prev = $code;
      }
    print LOG "_________________ statistics big _________________   \n";
  }


sub load_one_FT_Block_from_flugabluaf_and_process_count_only
  {
    my $self              = shift;
    my $root              = shift;
    my $file              = $self->{flugablauf_file};
	@{$self->{order_of_flights_in_ana}} = ();
    print STDERR "  * counting events in flugablauf:  \= $self->{flugablauf_file}\n";

    open(FLUGAB,  "<" . $file);
    my $type   = 0;
    my $no     = -1;

    while (<FLUGAB>)
      {
	chop($_);
	if (($_ =~m/^\s*$/) || ($_ =~m/^\s*\#/)) {print LOG "$_\n"; next;}

	my $line = $_;
	$line    =~s/^\s*//;

	if ($line =~m/^TF/)
	  {
	    $no++;
	    $type   = 1;

	    if ($no > 0)
	      {
		&process_the_current_flugablauf_block_count_only($self,$no);
		last if ($no > $self->{run_only_till_flight_num});  # do only these flights
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
    &process_the_current_flugablauf_block_count_only($self,$no);
	$self->{total_flight_types} = $no;
    close(FLUGAB);
    print STDERR "\n  * counting of Events Finished. \n";
  }



sub process_the_current_flugablauf_block_count_only
  {
    my $self     = shift;
    my $num      = shift;
    my $i        = 0;

    my $points = $self->{BLOCK}->{POINTS};
    my $valid  = $self->{BLOCK}->{VALID};
    my $block  = $self->{BLOCK}->{BLOCK};
    my @data   = @{$self->{BLOCK}->{DATA}};
    my @longcodes           = ();
    my $real_points = $#data + 1;
    @{$self->{DP_TF_ARRAY}} = ();
	@{$self->{TH_TF_ARRAY}} = ();
	$self->{mission_now}    = 100;
	my $catch_rotation      = 0;
	my $catch_mlg_impact    = 0;
	
    if (($self->{use_max_per_flight} > 0) && ($real_points < $self->{max_pointsper_flight})) {return;}

    if (($self->{count_flight_separate} > 0) && ($self->{use_max_per_flight} > 0))
      {
	$self->{statistics} = {}; $self->{stats_more}        = {};
	$self->{dp_max}     =  0; $self->{complete_validity} = 0;
      }

    $self->{BLOCK}->{HEAD} =~m/TF_(.+)\s*\(/;
    my $header = $1;
	push(@{$self->{order_of_flights_in_ana}}, $header); 

    $self->{complete_validity}       = $self->{complete_validity} + $valid * $block;
    $self->{total_points_all_flight} = $self->{total_points_all_flight} + $points * $valid * $block;
    print ERR sprintf("TF:%5i   valid:%5i   block:%3i   Points:%8i %-100s%s", 
		      "$num",  "$valid",  "$block",   "$points", "$self->{BLOCK}->{HEAD}","\n");
	
    if ($real_points != $points) {print ERR " *** ERROR: TFnum: $num [$header] Points_in_File: $points ! Use_Real_Points: $real_points \n";}
	$self->{global_start}++;  print STDERR sprintf("%12s","$header"); if ($self->{global_start} == 8) {$self->{global_start} = 0; print STDERR "\n";}
 
    @{$self->{GG_ORDER_THIS_FLIGHT}}            = ();	
	$self->{check_segment_sequence_this_flight} = {};
	unless (defined $self->{mlg_is_in_this_flight}->{$num}) {$self->{mlg_is_in_this_flight}->{$num} = 0;}
	unless (defined $self->{nlg_is_in_this_flight}->{$num}) {$self->{nlg_is_in_this_flight}->{$num} = 0;}

    foreach (@data)
      {
	my $line = $_;
	$line    =~s/^\s*//;
	if ($line =~m/[a-z]/) {print ERR " *** ERROR code ignored $line\n"; next;}
	my ($code,$coco,$dp,$temp) = split('\s+', $line);
	&analyze_coco_fourteen_position_count_only($self, $coco, $dp, $temp, $valid, $num);
	push(@{$self->{DP_TF_ARRAY}}, $dp); push(@{$self->{TH_TF_ARRAY}}, $temp); push (@longcodes, $coco);

	# check if flight starts/ends with a 1g
	if ($i == 0)      {unless ($coco =~m/0000000000$/) {push(@{$self->{global_considerations}}, " *** ERROR Flight does not START with 1g: $self->{BLOCK}->{HEAD}");};}
	if ($i == $#data) {unless ($coco =~m/0000000000$/) {push(@{$self->{global_considerations}}, " *** ERROR Flight does not END   with 1g: $self->{BLOCK}->{HEAD}");};}
	$i++;
	# check consistency of codes per line!
	my $lenA = length($code);  my $lenB = length($coco); #print STDERR "$lenA $lenB\n";
	unless (($lenA == 4) && ($lenB == 14))  {push(@{$self->{global_considerations}}, " *** Inconsistent Order of Codes |$line| $self->{BLOCK}->{HEAD}");}
	unless  ($coco =~m/^$code/)             {push(@{$self->{global_considerations}}, " *** Inconsistent CodeCoCo Found |$line| $self->{BLOCK}->{HEAD}");}
	unless (($dp =~m/\./)&&($temp =~m/\./)) {push(@{$self->{global_considerations}}, " *** DP/THER value not RealValue |$line| $self->{BLOCK}->{HEAD}");}                
	# check consistency of mission number for each line for the complete flight
	my $m_now = unpack('A1A3', $code);
	if ($self->{mission_now} == 100) {$self->{mission_now} = $m_now;}
	unless($m_now == $self->{mission_now}) {push(@{$self->{global_considerations}}, " *** ERROR Multi-MISSIONS in 1 FLIGHT |$line| $self->{BLOCK}->{HEAD}");}
	$self->{this_mission_has_thes_number_of_ftypes}->{$m_now}->{$num} = $valid;   # The number of flight types with this mission is stored here!
    if ($self->{check_4_return_2_1g} > 0)  # check the Rotation and MLG segments if they <Return to 1g>
	  {
        if ($catch_rotation < 2)
    	   {
    	    foreach (@{$self->{rotation_codes}})
              {
                my $suspect_code = $_ . '0000000000';  if ($coco =~m/$suspect_code/) {$catch_rotation++;} # First 1g Level
    	        if ($catch_rotation == 2) {push(@{$self->{back2_1g_considerations}},   sprintf("[%04s] [ROT] %-25s %-18s","$num","$header","$coco")); last;}
              }
    	   }
        if ($catch_mlg_impact < 2)
	       {	  
            foreach (@{$self->{mlg_impact_codes}})
              {
               my $suspect_code = $_ . '0000000000';  if ($coco =~m/$suspect_code/) {$catch_mlg_impact++;} # First 1g Level
	           if ($catch_mlg_impact == 2) {push(@{$self->{back2_1g_considerations}}, sprintf("[%04s] [MLG] %-25s %-18s","$num","$header","$coco")); last;}
              }
           }
	   }
	 my $lcode = 'NONE';  
     if (defined @{$self->{GG_ORDER_THIS_FLIGHT}}[0]) {my @a = @{$self->{GG_ORDER_THIS_FLIGHT}}; $lcode = $a[$#a];}	   
     unless (defined $self->{check_segment_sequence_this_flight}->{$code}) {push(@{$self->{GG_ORDER_THIS_FLIGHT}}, $code);}	
	 if    ((defined $self->{check_segment_sequence_this_flight}->{$code}) && ($lcode ne $code)) {push(@{$self->{GG_ORDER_THIS_FLIGHT}}, $code); print LOG "  *** ERROR Flight $num |$line| This SegCode happens 2-wice in same Flight!\n";} # This will Trigger Warnings!
     $self->{check_segment_sequence_this_flight}->{$code} = 1;	   
      } # end loop @data

    if (($self->{count_flight_separate} > 0) && ($self->{use_max_per_flight} > 0))
      {
	print LOG sprintf("TF:%5i   valid:%5i   block:%3i   Points:%8i %-100s%s",
			  "$num",  "$valid",  "$block",   "$points", "$self->{BLOCK}->{HEAD}","\n");
	&write_summary_statistics_only_BIG_flights($self);
	$self->{statistics_combi}  = {};   $self->{statistics_combi_count} = {}; @{$self->{back2_1g_considerations}} = ();
	@{$self->{DP_JUMP_WARN}}   = ();   @{$self->{DP_LOW_WARN}}         = (); @{$self->{ROUND_CLOCK_WARN_A}}   = (); @{$self->{ROUND_CLOCK_WARN_B}}   = ();
      }
    &check_the_DeltaPressure_profile($self, $num, $self->{mission_now}, $header);
    &check_if_segments_sequence_is_same_as_defined_in_TXT($self, $num, $self->{mission_now}, $header);
	
	unless (defined $self->{mission_ratio_mixed}->{$self->{mission_now}}) {$self->{mission_ratio_mixed}->{$self->{mission_now}} = 0;}
	$self->{mission_ratio_mixed}->{$self->{mission_now}} = $self->{mission_ratio_mixed}->{$self->{mission_now}} + $valid * $block;
	
	#check_round_the_clock_modifications_for_updown_sequence
	my $cin  = -1;  # counter

	unless ($self->{check_round_the_clock} < 1)
	  {
	foreach (@longcodes)
	  {
	    $cin++;
		next if (!defined $longcodes[$cin-1]); # last line
		last if (!defined $longcodes[$cin+1]); # next line
		my $digits = $_;                       # current line
	    my ($p,$sp1,$dp1,$sp2,$dp2) = unpack('A4A1A1A1A1', $longcodes[$cin-1]);
		my ($a,$sa1,$da1,$sa2,$da2) = unpack('A4A1A1A1A1', $digits);
	    my ($b,$sb1,$db1,$sb2,$db2) = unpack('A4A1A1A1A1', $longcodes[$cin+1]);
		my $codeA = $a . $da1; #2238 1
		my $codeB = $b . $db1; #2238 1
        #my $nameA = $self->{LCASE}->{$codeA}->{segname}->{$da1}; unless (defined $nameA)  {next;} 
        #my $nameB = $self->{LCASE}->{$codeB}->{segname}->{$db1}; unless (defined $nameB)  {next;}

		my ($e, $snP) = unpack('A2A2', $p); # Segment Number!
		my ($c, $snA) = unpack('A2A2', $a);  
		my ($d, $snB) = unpack('A2A2', $b);
		next unless (($snA == $snB) && ($snA == $snP));      # Check only within 1 Segment Number!  Remove line if transition checks also required!
		
		for (1 ..9) # 9 Severity Levels in the first Incre Position Only! 
		  {
		    my $step  = $_;
			my $next  = $step + 1;
		    if (($sa1 == $step) && ($da1 == 1))  # First Incre Position  Highest Severity (11) should be followed by 12 - or a half cycle may be lost.
		      {
		        unless (($sb1 == $step) && ($db1 == 2)) 
		    	  {$e = 1e6; 
			    my $t = " WARN  [$longcodes[$cin-1]] then [$digits] then [$longcodes[$cin+1]]";
			    push(@{$self->{ROUND_CLOCK_WARN_A}}, sprintf("[%04s] [%10s] %-75s","$num","$header","$t"));
		     	  }
		      }
			if (($sa1 == 0)  && ($da1 == 0) && ($e < 1e5)) 
		      {
		        if  (($dp1 == $step) && ($db1 == $next) && ($sp1 == $sb1)) 
		    	  {
			    my $t = " ERROR [$longcodes[$cin-1]] then [$digits] then [$longcodes[$cin+1]]";
			    push(@{$self->{ROUND_CLOCK_WARN_B}}, sprintf("[%04s] [%10s] %-75s","$num","$header","$t"));
		     	  }
		      }
		  }
	  }
	  }
  }


  
sub analyze_coco_fourteen_position_count_only
  {
    my $self   = shift;
    my $coco   = shift;
    my $dp     = shift;
    my $temp   = shift;
    my $valid  = shift;
	my $fnum   = shift;
    my @a      = ();

    $coco      =~m/(\d\d\d\d)(\d\d)(\d\d)(\d\d)(\d\d)(\d\d)/;

    $a[0]      =  $1;
    $a[1]      =  $2;
    $a[2]      =  $3;
    $a[3]      =  $4;
    $a[4]      =  $5;
    $a[5]      =  $6;
	
    if(length($coco) < 14) {print ERR "\n *** ERROR |$coco| not standard length!\n";}
	
    my $code     = shift(@a);
    my $gg_code  = $code . '0';

    if (defined $self->{max_dp_per_steady_1g}->{$gg_code})
      {
	my $v_max = $self->{max_dp_per_steady_1g}->{$gg_code};
	my $v_min = $self->{min_dp_per_steady_1g}->{$gg_code};
	if ($dp > $v_max)
	  {
	    $self->{max_dp_per_steady_1g}->{$gg_code} = $dp;
	  }

	if ($dp < $v_min)
	  {
	    $self->{min_dp_per_steady_1g}->{$gg_code} = $dp;
	  }
      }
    else
      {
	$self->{max_dp_per_steady_1g}->{$gg_code} = $dp;
	$self->{min_dp_per_steady_1g}->{$gg_code} = $dp;
      }

    unless (defined  $self->{statistics}->{GG}->{$gg_code}) {$self->{statistics}->{GG}->{$gg_code} = 0;}
    $self->{statistics}->{GG}->{$gg_code} = $self->{statistics}->{GG}->{$gg_code} + $valid;
	$self->{flights_validity_seen}->{$gg_code}->{$fnum} = $valid;

    my $i        = 0;
    my $counter  = 0;
    my $ex_1     = 0;
    my $ex_2     = 0;
    my $ex_3     = 0;

    if ($self->{dp_max} < $dp) {$self->{dp_max} = $dp;}

    if ($coco =~m/0000000000/)
      {
         if (defined $self->{gg_codes_alone}->{$gg_code}) {$self->{gg_codes_alone}->{$gg_code} = $self->{gg_codes_alone}->{$gg_code} + $valid;}
         else {$self->{gg_codes_alone}->{$gg_code} = $valid;}
      }

    foreach (@a)
      {
	$i++;
	my $ip        = $_;
	my ($n, $d)   = unpack('A1A1', $ip);
	my $incre     = $code . $i;
	my ($x_i,$y_i,$xy_i) = 0;

	if ($ip =~m/00/) {$ex_2++; next;}

	if (defined $self->{LCASE}->{$incre}->{issyno}->{$d})
	  {
	    my $issy = $self->{LCASE}->{$incre}->{issyno}->{$d};
	    my $fac  = @{$self->{LCASE}->{$incre}->{fac}->{$d}}[$n-1];
		
	    unless (defined $self->{statistics}->{$d}->{$incre}) {$self->{statistics}->{$d}->{$incre} = 0;};
	    unless (defined $self->{stats_more}->{$d}->{$n-1}->{$incre}) {$self->{stats_more}->{$d}->{$n-1}->{$incre} = 0;};
	    $self->{statistics}->{$d}->{$incre} = $self->{statistics}->{$d}->{$incre} + $valid;
	    $self->{stats_more}->{$d}->{$n-1}->{$incre} = $self->{stats_more}->{$d}->{$n-1}->{$incre} + $valid;
	    $ex_1++;
	    $counter++;
		
	    my $seefac = abs($fac);
	    if ($seefac > $self->{cvt_factor_warn}) {print ERR " ** WARN CVT Factor: $fac  $coco  $incre \n";}
		$self->{flights_validity_seen}->{$d}->{$incre}->{$fnum} = $valid;
	  }

	if (($d == 2) && (!defined $self->{LCASE}->{$incre}->{issyno}->{2}))
	  {
	    if (defined $self->{LCASE}->{$incre}->{issyno}->{1}) 
	      {
		my $issy = $self->{LCASE}->{$incre}->{issyno}->{1};
		my $fac  = @{$self->{LCASE}->{$incre}->{fac}->{1}}[$n-1] * -1.0;

		unless (defined $self->{statistics}->{$d}->{$incre}) {$self->{statistics}->{$d}->{$incre} = 0;};
		unless (defined $self->{stats_more}->{$d}->{$n-1}->{$incre}) {$self->{stats_more}->{$d}->{$n-1}->{$incre} = 0;};
		$self->{statistics}->{$d}->{$incre}  = $self->{statistics}->{$d}->{$incre} + $valid;
		$self->{stats_more}->{$d}->{$n-1}->{$incre} = $self->{stats_more}->{$d}->{$n-1}->{$incre} + $valid;
		$ex_1++;
		$counter++;

		my $seefac = abs($fac);
		if ($seefac > $self->{cvt_factor_warn}) {print ERR " ** WARN CVT Factor: $fac  $coco  $incre \n";}
		$self->{flights_validity_seen}->{$d}->{$incre}->{$fnum} = $valid;
	      }
	  }

	my $nl_incre = $code . $i . $ip;
	if ($ex_1 > 0) {$ex_3 = 2}

	if (defined $self->{LCASE}->{$nl_incre}->{issyno}->{1})
	  {
	    my $issy = $self->{LCASE}->{$nl_incre}->{issyno}->{1};
	    my $fac  = @{$self->{LCASE}->{$nl_incre}->{fac}->{1}}[0];

	    if ($ip =~m/2$/) # Maxdam adds a -1 again to NLC of the 2nd direction!!!!!!!
	      {
		unless ($fac == -1)
		    {
			   if ($self->{switch_off_D2_error} < 1) 
			     {
				   push(@{$self->{global_considerations}}, " *** GLOBAL WARNING Check your NL D-2 Factors e.g. |$nl_incre| factor is not -1 ?"); 
			       $self->{switch_off_D2_error} = 1; 
				 }
			}
	      }

	    $ex_1++;
	    unless (defined $self->{statistics}->{$d}->{$nl_incre}) {$self->{statistics}->{$d}->{$nl_incre} = 0};
	    $self->{statistics}->{$d}->{$nl_incre} = $self->{statistics}->{$d}->{$nl_incre} + $valid;
	    if ($ex_3 > 2) {print ERR " ** WARN Linear and NL Cases are Combined: $coco \n";}
	    $counter++;

	    my $seefac = abs($fac);
	    unless ($seefac == 1) {print ERR " ** ERROR CVT NonLinear Factor should be +/- 1.0: $fac  $coco  $nl_incre \n";}
		$self->{flights_validity_seen}->{$d}->{$nl_incre}->{$fnum} = $valid;
				
        my $segname = $self->{LCASE}->{$nl_incre}->{segname}->{1};	#print LOG " FOUND MLG $self->{mlg_is_in_this_flight}->{$fnum}\n";
        if (($segname =~m/\_LMLG/) || ($segname =~m/\_MLG/) || ($segname =~m/\_BWLG/)) {$self->{mlg_is_in_this_flight}->{$fnum} = $self->{mlg_is_in_this_flight}->{$fnum} + 1;}
        if  ($segname =~m/\_LNLG/) {$self->{nlg_is_in_this_flight}->{$fnum} = $self->{nlg_is_in_this_flight}->{$fnum} + 1;}
      }
      }

    if (($ex_1 < 1) && ($ex_2 < 5))
      {
	print ERR " *** ERROR $coco not interpreted!\n";
	print LOG " *** ERROR $coco not interpreted!\n";	
      }

    if ($counter > 1)
      {
	my $i = 0;  my $a  = 'A';  #print LOG "  * more than 1 linear incre are combined in this coco: $coco\n";
	foreach (@a)
	  {
	    $i++;
	    my $ip        = $_; if ($ip =~m/00/) {next;}
	    my ($n, $d)   = unpack('A1A1', $ip);
	    my $li_incre  = $code . $i; 
		my $nl_incre  = $code . $i . $ip;
		my $incre     = 'A';
		if ((defined $self->{LCASE}->{$nl_incre}->{issyno}->{1}) && ($self->{LCASE}->{$nl_incre}->{issyno}->{1} > 0))  {$incre = $nl_incre;}		
		if ((defined $self->{LCASE}->{$li_incre}->{issyno}->{1}) && ($self->{LCASE}->{$li_incre}->{issyno}->{1} > 0))  {$incre = $li_incre;}
		
	    if ((defined $self->{LCASE}->{$incre}->{issyno}->{1}) && ($self->{LCASE}->{$incre}->{issyno}->{1} > 0))
	      {
		   $self->{statistics_combi}->{$incre}->{$a} = 1;
		   unless ($a eq 'A')
		     {
		       unless (defined $self->{statistics_combi_count}->{$incre}->{$a}) {$self->{statistics_combi_count}->{$incre}->{$a} = 0;}
		       $self->{statistics_combi_count}->{$incre}->{$a} = $self->{statistics_combi_count}->{$incre}->{$a} + $valid;
		     }
		   $a = $incre;
	      } 
	  } # end @a
      } # end counter > 1
  } # end subroutine



sub load_loadcase_data_into_structure_count_only
  {
    my $self  = shift;
    my $file  = $self->{loadcase_file};
    my $i     = 0;
    my $j     = 0;
    my $type  = 'N';
	my $k     = 1;
    my $pcode = '0000'; my $pissy = '0000'; my $psign = 1; my $pseg = 'SEG';
    open( INPUT, "< $self->{index_file}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    @{$self->{LCASE}->{HEADER}} = ();
    $self->{LCASE}              = {};
    @{$self->{ALLCODES}->{ALLCODES}} = ();
    @{$self->{ISSYNUM}->{NUMBERS}}   = ();
    @{$self->{ALLCODES}->{ALLNAMES}} = ();	
	for (1 ..9) {@{$self->{ALLCODES}->{GG_ORDER}->{$_}} = ();}
	
    foreach (@contents)
      {
	my $line  = $_;
	if ($_ =~m/^\s*$/) {next;}
	if ($_ =~m/^\s*#/) {push(@{$self->{LCASE}->{HEADER}}, $_); print VAL "$_\n"; next;}
	$line      =~s/^\s*//;
	$j = 0;
	my @array   = ();
	my $segname;
	my @all     = split('\s+', $line);
	if ($all[0] =~m/EXC|N|\_/)
	  {
	    $segname = shift(@all);  $segname =~s/\s*//g;
	    @array   = @all; 
	  }
	elsif($all[0] =~m/START\_/)      
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 1) {shift(@all);};
	    @array   = @all; 
	  }	
    elsif($all[0] =~m/START/i)      
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 3) {shift(@all);};
	    @array   = @all; 
	  } 
	else
	  {
	    $segname = 'segment_' . $i; $self->{txt_cvt_esg_switch}    = 'CVT';
	    @array   = @all;
	  }
	
	# EXC_N=1_TXOT_F1      1GTXOT___4      11010 508  # Just for the A340/A330 ESG Project  !
	if ($self->{esg_switch} > 0) {shift(@array); $self->{txt_cvt_esg_switch} = 'ESG';};
	
	foreach (@array)
	  {
	    $array[$j] =~s/\s*//g;
	    $j++;
	  }
	
	for (0 .. 8) {if ((!defined $array[$_]) || ($array[$_] !~m/\d+/)) {$array[$_] = '-0-';}}
	
	my $code       = shift(@array);
	my $issynum    = shift(@array);
	my @factors    = @array;
	my ($mission,$gf,$snum,$pos)    = unpack('A1 A1 A2 A1', $code); #$issynum = $mission . $issynum;
	if ($segname =~m/segment_/) {$segname = $mission . '_segment_' . $snum;}
		
	unless ((defined $code) && (defined $issynum)) {next;}
	$i++;
	if ($code =~m/0$/)   {$self->{STEADY}->{$code}    = $issynum;  unless($code =~m/000$/) {push(@{$self->{ALLCODES}->{GG_ORDER}->{$mission}}, $code);}}
	if ($code =~m/000$/) {$self->{DPCASE}->{$mission} = $issynum;}
	if (defined $self->{LCASE}->{$code}) {$k = $self->{LCASE_K}->{$code} + 1;} else {$k = 1;}     # find first time! & find other times!
    if ((defined $self->{LCASE}->{$code}) && ($pcode != $code)) {print LOG sprintf("%68s |%-110s| %-45s%s","  ** ERROR Can ISAMI inteprete Position/Order of this CODE in TXT/CVT","$line","Warning may be caused by another Reason!","\n");}
	if ($pos > 5) {print LOG sprintf("%68s |%-110s| %-45s%s","  ** ERROR This INCRE Position greater than 5!","$line","CLASS has max 5 INCRE positions!","\n");}
	$self->{LCASE}->{$code}->{mission}->{$k} = $mission;
	$self->{LCASE}->{$code}->{issyno}->{$k}  = $issynum;
	$self->{LCASE}->{$code}->{code}->{$k}    = $code;
	$self->{LCASE}->{$code}->{segname}->{$k} = $segname;
    $self->{LCASE_K}->{$code}                = $k;           # HTP GLA order may not be as others!
	@{$self->{LCASE}->{$code}->{fac}->{$k}}  = @factors;
	push(@{$self->{ALLCODES}->{ALLCODES}},  $code);
	push(@{$self->{ALLCODES}->{ALLNAMES}},  $segname);	
	push(@{$self->{ISSYNUM}->{NUMBERS}}, $issynum);
	######
	unless (defined $self->{know_all_unique_issy_num}->{$issynum}) {$self->{know_all_unique_issy_num}->{$issynum} = 1;} else {$self->{know_all_unique_issy_num}->{$issynum} = $self->{know_all_unique_issy_num}->{$issynum} + 1;}
	push(@{$self->{issy_num_unique_array}->{$issynum}}, $code);	
	push(@{$self->{class_code_unique_array}->{$code}}, $segname);
	push(@{$self->{issy_num_unique_segnames}->{$issynum}}, $segname);	
	######
	my $length = length($code);	
	######
	if ($length == 5)   # check how many Events are attached to this 1g 
	  {
	    my ($core, $pos) = unpack('A4 A1', $code);  # 22012
		unless (defined $self->{LINEAR_CODES}->{$core})  {$self->{LINEAR_CODES}->{$core} = 1;} else {$self->{LINEAR_CODES}->{$core} = $self->{LINEAR_CODES}->{$core} + 1;}
		push(@{$self->{linear_codes_segnames}->{$core}}, sprintf("%-18s","$segname"));
		if ($pos > 5) {print LOG sprintf("%68s |%-110s| %s","  *** ERROR INCRE in Position |$pos| - CLASS has MAX 5 INCRE!!!!!!","$line", "\n");}
	  }	
	######
    if ($length > 5)
	  {
	    my ($core, $pos, $x, $d) = unpack('A4 A1 A1 A1', $code);  # 2201211
		my ($c,    $incre)       = unpack('A5 A2',       $code);  # 2201212
		$self->{NL_CODES}->{$code}    = @{$self->{LCASE}->{$code}->{fac}->{1}}[0]; # this is the factor!;
		$self->{NL_UNIQUE}->{$core}->{$pos}->{$d}->{$incre}    = $code;
	  }
	######
	my $flg = 0;
	my $ccc = -1;
	if ($k > 1)
	   {
		 if ((defined @{$self->{LCASE}->{$code}->{fac}->{$k}}[0]) && (defined @{$self->{LCASE}->{$code}->{fac}->{$k-1}}[0]))
		   {
		      my @f_now  = @{$self->{LCASE}->{$code}->{fac}->{$k}};
		      my @f_last = @{$self->{LCASE}->{$code}->{fac}->{$k-1}};
			  foreach (@f_now)
			    {
				   $ccc++;
				   my $fac_1 = $_;
				   my $fac_2 = $f_last[$ccc];
				   next if (($fac_1 =~m/-0-/) || ($fac_2 =~m/-0-/));
			       my $a = abs($fac_1);    my $b = abs($fac_2);
			       unless ($a == $b) {$flg++;}				   
				}
			  if (($flg > 0) && ($pseg =~m/$segname/) && ($pseg !~m/\_VG/)) {push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** ERROR %-21s    ---> UnSyM FacTors for this Segment found |%-55s|%-50s|","$segname","@f_last","@f_now"));}
			  if (($flg > 0) && ($pseg =~m/$segname/)) {push(@{$self->{LCASE_TXT_CVT_WARN_EXTRA}}, sprintf("  ** WARN  %-21s    ---> UnSyM FacTors for this Segment found |%-55s|%-50s|","$segname","@f_last","@f_now"));}				   
		   }
	   }
	$ccc     = -1;   $flg = 0;
	my $prv  = 10000000;	my $sign = 1;
	foreach(@factors)
	   {
          next if ($_ =~m/-0-/);  if ($_ <  0) {$sign = -1;}
		  my $a = abs($_);
		  unless ($a < $prv) {$flg++;}
		  $prv  = $a;
	   }                      
    unless ($flg == 0) {push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** ERROR %-21s    ---> Sequence for Factors Un-Expected     %-55s","$segname","|@factors|"));}
	
	###### We have same Class Code but different ISSYNUMs BUT we also have negative SIGNs for the 2nd direction?  Why this Overkill ? What is in ConvTable written?
	if (($pcode == $code) && ($pissy != $issynum) && ($sign != $psign) && ($pseg =~m/$segname/)) # EXC_GYTOAR_F1        12021   1466    1.455    1.351    1.143    0.935    0.727    0.520    0.312    0.104
	  {                                                                                          # EXC_GYTOAR_F1        12021   1467   -1.455   -1.351   -1.143   -0.935   -0.727   -0.520   -0.312   -0.104
          push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** CONFLICT %-21s ---> Same ClassCode But [Diff. ISSY & Diff. Sign +/- Directions] CHECK ConvTABLE how this is Explained for USER!","$segname"));	     
	  }
	if (($pcode == $code) && ($pissy == $issynum) && ($sign == $psign) && ($pseg =~m/$segname/)) # EXC_GYTIAR_F1        12351   1498    1.455    1.351    1.143    0.935    0.728    0.520    0.312    0.104
	  {                                                                                          # EXC_GYTIAR_F1        12351   1498    1.455    1.351    1.143    0.935    0.728    0.520    0.312    0.104
          push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** CONFLICT %-21s ---> Same ClassCode & Same ISSY & Same SIGN - WHY DO YOU NEED THIS? CHECK ConvTABLE how this is Explained for USER!","$segname"));	     
	  }	  
	$pcode  = $code; $pissy = $issynum;	$psign = $sign; $pseg = $segname;
      }
    $self->{LCASE}->{TOTAL} = $i;
	
	&check_2nd_direction_nl_codes_factors($self);
  }
  
  
sub check_2nd_direction_nl_codes_factors  # Factor for D2 should be -1 if only 2 sets available. If more directions then all factors should be 1.
  {
     my $self  = shift;
     foreach (sort keys %{$self->{NL_UNIQUE}})
	   {
	     my $core = $_;
		foreach (sort keys %{$self->{NL_UNIQUE}->{$core}})
	      {
		    my $pos = $_;
			my @d      = keys %{$self->{NL_UNIQUE}->{$core}->{$pos}};
			my $nf     = $#d + 1;   my $z = $core . $pos . 'x' . $d[0];
            unless ($nf > 1) {push(@{$self->{global_considerations}}, "  **  WARN No pair for NL_CODE core |$z| only SINGLE direction seen!"); next;}

		    foreach (@d)
		        {
		           my $d    = $_;
				   my @incr = keys %{$self->{NL_UNIQUE}->{$core}->{$pos}->{$d}};
				   #next unless ($d == 2)
				   foreach (@incr)
				     {
					   my $incre = $_;
					   my $code  = $core . $pos . $incre;
					   my $fac  = $self->{NL_CODES}->{$code};
			           if (($d == 2) && ($nf >  2) && ($fac <  1))  {push(@{$self->{global_considerations}}, "  *** ERROR NL_CODE factor |$code| must be  1.0 ! ");} # 
			           if (($d == 2) && ($nf == 2) && ($fac > -1))  {push(@{$self->{global_considerations}}, "  *** ERROR NL_CODE factor |$code| must be -1.0 ! ");} # 
			           if (($d != 2) && ($nf >  0) && ($fac <  1))  {push(@{$self->{global_considerations}}, "  *** ERROR NL_CODE factor |$code| must be  1.0 ! ");} # 			  
		             }		 
				}
		  }	  
	  }
  }


  
sub check_if_same_ISSY_number_used_for_MORE_than_one_Event 
  {
    my $self  = shift;  
	
    foreach (sort keys %{$self->{know_all_unique_issy_num}})  
	  {
	    my $issynum = $_;
		my @array_codes    = @{$self->{issy_num_unique_array}->{$issynum}};
		my @array_segnames = @{$self->{issy_num_unique_segnames}->{$issynum}};
		my $i = 0;
		foreach (@array_codes)            # Check ISSY vs. CODES
		  {
		    $i++;
			my $code = $_;
		    unless (defined $array_codes[$i]) {last;}  
			#if ($issynum == 2490) {print LOG "|$issynum|$code|$array_codes[$i]|\n";}
		    unless ($code == $array_codes[$i])    {push(@{$self->{LCASE_TXT_CVT_WARN}}, "  *** WARN  Multiple Usage of ISSYNUM |$issynum| for CODES    |@array_codes|");}
		  }
		$i = 0;  
		foreach (@array_segnames)         # Check ISSY vs. SEGNAMES
		  {
		    $i++;
			my $segname = $_;
		    unless (defined $array_segnames[$i]) {last;}
		    unless ($segname =~m/$array_segnames[$i]/) {push(@{$self->{LCASE_TXT_CVT_WARN}}, "  *** ERROR Multiple Usage of ISSYNUM |$issynum| for SEGNAMES |@array_segnames|");}
		  }
	  } # end foreach	

    foreach (sort keys %{$self->{know_all_unique_issy_num}})   # Check CODES vs. SEGNAMES
	  {
	    my $issynum = $_;
		my @array_codes = @{$self->{issy_num_unique_array}->{$issynum}};
		
		foreach (@array_codes)
		  {
			my $code = $_;
			my @array_segnames = @{$self->{class_code_unique_array}->{$code}};
		    my $i = 0;	
			foreach (@array_segnames)
			  {
			  	$i++;
                my $segname = $_;				
		        unless (defined $array_segnames[$i]) {last;}
		        unless ($segname =~m/$array_segnames[$i]/) {push(@{$self->{LCASE_TXT_CVT_WARN}}, "  *** ERROR Multiple Usage of CODE    |$code| for SEGNAMES |@array_segnames|");}				
			  }
		  }
	  } # end foreach
	  
	foreach(@{$self->{ALLCODES}->{ALLCODES}})   # Array CODE: Segname    used for Checks if Same 1g Code and Segname Together is used more than once
	  {
	     my $code      = $_;
		 unless ($code =~m/0$/)   {next;}
		 if     ($code =~m/000$/) {next;}
		 my $segname   = $self->{LCASE}->{$code}->{segname}->{1}; 
		 my $issynum   = $self->{LCASE}->{$code}->{issyno}->{1};
		 my @array_1g_segnames  = @{$self->{class_code_unique_array}->{$code}};
		 my @array_codes        = @{$self->{issy_num_unique_array}->{$issynum}};
		 if (defined $array_1g_segnames[1]) {push(@{$self->{LCASE_TXT_CVT_WARN}}, "  *** ERROR Multiple Usage of 1g SEGNAME    |@array_1g_segnames|  with same CODE  |$code| ");}
         if (defined $array_codes[1])       {push(@{$self->{LCASE_TXT_CVT_WARN}}, "  *** ERROR Multiple Usage of 1g CODE       |@array_codes| with Same SEGNAMES |@array_1g_segnames|");}
	  }
	
    # This part uses the Average increments found for Flight Segments to compare if every segment has this average. Normally a small imbalance and it complains about all seqments.    # Example Checks if a flight segment is missing LG incre BUT all other segments have LG? Why? Maybe just forgotten?   It knows a flight segment if it finds VG or HOG is defined for the segment.
	foreach (sort keys %{$self->{LINEAR_CODES}})
      {
         my $core  = $_;
		 my @names = @{$self->{linear_codes_segnames}->{$core}};
		 foreach (@names) {my $segname = $_; if ($segname =~m/EXC\_VG|EXC\_HOG/i) {$self->{LINEAR_CODES_FLIGHT}->{$core} = 1;} }
      }	  
	my $j   = 0; 
    my $sum = 0;	
	foreach (sort keys %{$self->{LINEAR_CODES_FLIGHT}})
      {
         my $core      = $_;
         $sum          = $sum + $self->{LINEAR_CODES}->{$core};
		 $j++;
      }
	my $average = 0;  
    unless($j == 0) {$average = $sum / $j;  print STDERR "\n<$average>\n";} 
	foreach (sort keys %{$self->{LINEAR_CODES_FLIGHT}})
      {
         my $core      = $_;
         my $num_incre = $self->{LINEAR_CODES}->{$core};
		 my @names = @{$self->{linear_codes_segnames}->{$core}};
		 if ($num_incre < $average) {push(@{$self->{LCASE_TXT_CVT_WARN}}, "  *** WARN  Expected MORE Increments for CORE |$core| ONLY FOUND SEGNAMES |@names|");}
		 if ($num_incre > $average) {push(@{$self->{LCASE_TXT_CVT_WARN}}, "  *** WARN  Expected LESS Increments for CORE |$core| MORE FOUND SEGNAMES |@names|");}
      }		    
  }  # end sub
  

  
sub check_if_the_segment_number_of_INCRE_is_same_as_the_STEADY_segment_number  # e.g. Do we have EXC_VG8_F2 in Steady segment EXC_N=1_32_s_F2!
  {
    my $self           = shift;
	my $steady_num     = 0;
	my $incre_num      = 1;
	my @names          = @{$self->{ALLCODES}->{ALLNAMES}};
	
	foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
	    my $code      = $_;	
	    my $segname   = shift(@names);
		next if($code =~m/000$/);
		
		if ($segname =~m/_N\=1_/)
		   {
		      $segname =~m/.*N\=1_(.+).+F\d+$/;
		      $steady_num = $1;
			  $steady_num =~s/[a-z]//g;
			  $steady_num =~s/[A-Z]//g; 
			  $steady_num =~s/-|_//g;
			  next;
		   }
		   
		$segname   =~m/EXC_(.+)_.*F\d+$/; 
		$incre_num = $1; 
		$incre_num =~s/[a-z]//g; 
		$incre_num =~s/[A-Z]//g; 
		$incre_num =~s/-|_//g;
		next unless ((defined $incre_num)  && ($incre_num  =~m/\d+$/));
		next unless ((defined $steady_num) && ($steady_num =~m/\d+$/));
		next unless ($segname =~m/VG|LG|LM|CT|VM/i);
		unless ($steady_num == $incre_num) {push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** CONFLICT XXX %-21s STEADY[%2s] vs. INCRE[%2s] ---> STEADY & INCRE Segment Numbers do not Strictly Match!","$segname","$steady_num","$incre_num"));}
	  }	
  } # end sub	

  
  
sub check_how_many_times_the_same_loadcase_code_occurs_in_the_Mission 
  {
    my $self  = shift;
	my $prev  = '0000';
	my @once  = ('GXLR', 'GXTO','LMLG',  'BWLG',  'GROTAT',);  # NL cases are expected to be seen 1once uniquely!
	my @twice = ('VG',   'LG',  'GYTOAR','GYTOAL','GYTIAR','GYTIAL','GZTO90', 'GLIDUM','GZLR30','GZLR70','GZTR','GZTO90','GZTI90'); 
	my @four  = ('GZTI05','GZTO05','GYTOA','GYTIA','GZTO50','GZTI50',);
	
	foreach (@twice)
	  {
	    my $event_name = $_;
	    foreach (@{$self->{ALLCODES}->{ALLCODES}})
          {
	        my $code   = $_;	
	        if ($prev == $code)   {next;}
	        my $segname   = $self->{LCASE}->{$code}->{segname}->{1};
		    my $event_num = $self->{LCASE_K}->{$code};
			
			if($segname =~m/\_$event_name/)
			  {
			    unless($event_num == 2) {push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** CONFLICT %-21s %-9s ---> Event Expected ONLY TWICE! But now seen %2s Times!","$segname","$code","$event_num"));}
			  }
            $prev = $code;
		  }
	  }	# end twice
	  
	foreach (@four)
	  {
	    my $event_name = $_;
	    foreach (@{$self->{ALLCODES}->{ALLCODES}})
          {
	        my $code   = $_;	
	        if ($prev == $code)   {next;}
	        my $segname   = $self->{LCASE}->{$code}->{segname}->{1};
		    my $event_num = $self->{LCASE_K}->{$code};
			
			if($segname =~m/\_$event_name/)
			  {
			    unless($event_num == 4) {push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** CONFLICT %-21s %-9s ---> Event Expected FOUR Times! But now seen %2s Times!","$segname","$code","$event_num"));}
			  }
            $prev = $code;
		  }
	  }	# end four	  

	foreach (@once)
	  {
	    my $event_name = $_;
	    foreach (@{$self->{ALLCODES}->{ALLCODES}})
          {
	        my $code   = $_;	
	        if ($prev == $code)   {next;}
	        my $segname   = $self->{LCASE}->{$code}->{segname}->{1};
		    my $event_num = $self->{LCASE_K}->{$code};
			
			if($segname =~m/\_$event_name/)
			  {
			    unless($event_num == 1) {push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** CONFLICT %-21s %-9s ---> Event Expected ONE Time! But now seen %2s Times!","$segname","$code","$event_num"));}
			  }
            $prev = $code;
		  }
	  }	# end once	  
  }

  
sub warn_when_expected_combinations_do_not_exist
  {
    my $self  = shift;

    my @combinations = ('GXTO','GXTI','GXLR','VG','LG','GYTO','GYTI','GYTOAL','GYTOAR','GYTOA','GZTO05','GZTI05','GZTO10','GZTI10','GZTO50','GZTI50','GYTIAL','GYTIAR','GYTIA','GZLR'); # WARN about these Combinations (if not available)

    foreach (sort keys %{$self->{statistics_combi}}) 
      {
         my $key    = $_;  # $code
         foreach (sort keys %{$self->{statistics_combi}->{$key}}) # $self->{statistics_combi}->{$incre}->{$a}
           {
             my $value  = $_;  # $code
             my $koccur = $self->{statistics_combi_count}->{$key}->{$value}; 
             my $kall   = $self->{total_occur_per_code}->{$key};
             my $vall   = $self->{total_occur_per_code}->{$value};
             my $name_a = 'A'; my $name_b = 'B'; 
             if (defined $self->{LCASE}->{$key}->{segname}->{1})   {$name_a = $self->{LCASE}->{$key}->{segname}->{1};}
             if (defined $self->{LCASE}->{$value}->{segname}->{1}) {$name_b = $self->{LCASE}->{$value}->{segname}->{1};}
			 
             my $m      = unpack('A1', $value);
			 
			 if ($name_a =~m/EXC/)
			   {
			     $name_a =~m/EXC_(.+)_F.+/;      #  EXC_GYTOAL_F2  or  EXC_VG8_F2
				 my $usename = $1;
			     foreach (@combinations) 
			      {
			        my $event = $_;
			        if ($usename =~m/$event/) {if (defined $self->{secured_combined}->{$m}->{$event}) {$self->{secured_combined}->{$m}->{$event} = $self->{secured_combined}->{$m}->{$event} + 1;} else {$self->{secured_combined}->{$m}->{$event} = 1;}}
			      }
			   }
			 if ($name_b =~m/EXC/)
			   {
			     $name_b =~m/EXC_(.+)_F.+/;
				 my $usename = $1;
			     foreach (@combinations) 
			      {
			        my $event = $_;
			        if ($usename =~m/$event/) {if (defined $self->{secured_combined}->{$m}->{$event}) {$self->{secured_combined}->{$m}->{$event} = $self->{secured_combined}->{$m}->{$event} + 1;} else {$self->{secured_combined}->{$m}->{$event} = 1;}}
			      }
			   }
          } # keys 
      } # end foreach loop
	 
	  foreach (@combinations)
	    {
		  my $event = $_;
		  my $value = 0;
		  my $line  = '  *** INFO ';
		  foreach (sort keys %{$self->{mission_ratio_mixed}})
		    {
			  my $m = $_;
			  if (defined $self->{secured_combined}->{$m}->{$event}) {$value = $self->{secured_combined}->{$m}->{$event};}
              $line = $line . sprintf(" %10s is combined %3s Times [M%s]","$event","$value","$m");  
		   }
		  push(@{$self->{LCASE_TXT_CVT_WARN}}, $line); 
		}
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




# print STDERR "  * Counts all events in the Flugablauf using the Loadcase.txt as a guide! \n";
# print STDERR "  * Also Checks which incre-Events are combined together! \n";
# print STDERR "  * Counts all Linear steps 1-9 and all non-linear Steps 1-9! \n";
# print STDERR "  * added Options -->> Count only BiG Flights i.e > 30000 points?\n";
# print STDERR "  *               -->> Checks DP distribution && %%% Combinations\n";
# print STDERR "  *               -->> Checks Rules for VG&LG Unsymmetries & Prints now ISSYNUM & Un-Comb Stats per Flight\n";
# print STDERR "  *               -->> Checks if ANA File Codes per Line are Consistent\n";
# print STDERR "  *               -->> Checks ->Flights Start/End with 1g? <- &-> Round the Clock<- Modifications\n";
# print STDERR "  *               -->> Checks if Multiple-Missions are in a Single Flight\n";
# print STDERR "  *               -->> Checks #Return to 1g# for {Rotation} and {MLG-Impact}\n";
# print STDERR "  *               -->> Additional Counts for 1g Alone - required for FSFT Spectra!\n";
# print STDERR "  *               -->> NL loadcases direction Checks -1/1 - Mainly for HL&MOvables Spectra!\n";
# print STDERR "  *               -->> Additional Checks for usages found for combination of ISSYNUM & CODES & SEGNAMES!\n";
# print STDERR "  *               -->> Checks for Multiple Usage of same 1g Segments & Codes!\n";
# print STDERR "  *               -->> v6.2 Checks for OrDER of Flights and Missing Flights - ISAMI compatibility!\n";
# print STDERR "  *               -->> Count flights Together or Separately?\n\n";

# my $self               = {};
# $self->{tool_version}  = '8.0';
# $self->{date}          = '17.08.2017';

# $self->{index_file}            = 'A350XWB900CS-111HTPBCFRP-MX3.txt';  # #'loadcases_C26.txt'; 'A350XWB900-526NF-MR3.txt'
# $self->{flugablauf_file}       = 'A350XWB900CS-111HTPBCFRP-MX3.ana';  # #'flugablauf_C26.ana'; 'A350XWB900-526NF-MR3.ana';

# # For A380 MRB
# #my @rotation_codes             = ('1107','2107','3107','4107');
# #my @mlg_impact_codes           = ('1153','2153','3153','4153');

# # For A380 TC
# #my @rotation_codes             = ('2104','5104','8104');
# #my @mlg_impact_codes           = ('2238','5238','8238');

# # For -1000 TL Only
# #my @rotation_codes             = ('1110','4110','7110','2110','5110','8110','3110','6110','9110');  # 4digit Class Code for Rotation
# #my @mlg_impact_codes           = ('1181','4181','7181','2181','5181','8181','3181','6181','9181');  # 4digit Class Code for MLG Impact

# # For -900 TL/CS Only
# #my @rotation_codes             = ('1105','4105','7105','2105','5105','8105','3105','6105','9105');  # 4digit Class Code for Rotation
# #my @mlg_impact_codes           = ('1133','2138','3144','4133','5138','6144','7133','8138','9144');  # 4digit Class Code for MLG Impact

# # For -1000 PTL ONLY
# #my @rotation_codes             = ('2121','5121','8121');  # 4digit Class Code for Rotation
# #my @mlg_impact_codes           = ('2150','5150','8150');  # 4digit Class Code for MLG Impact



1;
