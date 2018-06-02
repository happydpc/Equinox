package CsFactorPure;


use strict;

use Tk;

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
    my $self = shift;

    if (@_)  
      {
        my %extra = @_;
        @$self{keys %extra} = values %extra;
      }

    my $root            = $self->{ROOT};
    $self->{FILE}       = $root->{FILE};
    $self->{DSG}        = $root->{validity};
    $self->show_options_for_user_input;
  }



sub show_options_for_user_input
  {
    my $self   = shift;
    my $root   = $self->{ROOT};

    my $top    = $root->{MW}->Toplevel();
                $top->title("CS factor Pure v1.000 !");

    my $frame  = $top->Frame(
			    -relief => 'groove',
			    -borderwidth => 5,
			   )->pack(
				   -side   => 'top',
				   -fill   => 'x',
				  );
    my $fr_fai = $frame->Frame(
			       -relief => 'groove',
			       -borderwidth => 2,
			      )->pack(
				      -side   => 'top',
				      -fill   => 'x',
				     );

    $self->{cppfile}          = $root->{FILE};
    $self->{stress_reference} = 100.0;
    $self->{IQF}              = 120.0;
    $self->{factor}           = 1.0;
    $self->{p}                = 4.5;
    $self->{q}                = 0.6;
    $self->{INFO}             = 'CS factor ref. MTS005. Complete entries to calculate.';

    my $fai_entry =
      $self->{fai_entry} = 
	$fr_fai->EuLabEntry(
			    -label        => sprintf("%-20s%5s%s","Pure Input File","[ ]","*"),
			    -labelPack    => [-side => "left", -anchor => "w"],
			    -width        => 40,
			    -textvariable => \$self->{cppfile},
			   )->pack(
				   -side   => 'left',
				   -anchor => 'nw',
				   -fill   => 'x',
				   -padx   => 10,
				   -pady   => 2,
				  );
    my $browse =
      $self->{B_BROW}
	= $fr_fai->Button(
			  -text             => 'browse',
			  -command          => sub 
			  {
			    $self->{OPERATION} = 'open';
			    $self->fileDialog;
			    $self->{cppfile}   =  $self->{FILE};
			  }
			 )->pack(
				 -side   => 'right',
				 -anchor => 'e',
				 -padx   => 2,
				 -pady   => 2,
				);
    my $ref_entry =
      $self->{ref_entry} = 
	$frame->EuLabEntry(
			 -label        => sprintf("%-20s%5s%s","reference stress","[Mpa]","*"),
			 -labelPack    => [-side => "left", -anchor => "w"],
			 -width        => 20,
			   -numberonly   => '1',
			 -textvariable => \$self->{stress_reference},
			)->pack(
				-side   => 'top',
				-anchor => 'nw',
				-fill   => 'x',
				-padx   => 10,
				-pady   => 2,
			       );
    my $fac_entry =
      $self->{fac_entry} = 
	$frame->EuLabEntry(
			 -label        => sprintf("%-20s%5s%s","overall factor","[ ]","*"),
			 -labelPack    => [-side => "left", -anchor => "w"],
			 -width        => 20,
			   -numberonly   => '1',
			 -textvariable => \$self->{factor},
			)->pack(
				-side   => 'top',
				-anchor => 'nw',
				-fill   => 'x',
				-padx   => 10,
				-pady   => 2,
			       );
    my $p_entry =
      $self->{p_entry} = 
	$frame->EuLabEntry(
			 -label        => sprintf("%-20s%5s%s","material p","[ ]","*"),
			 -labelPack    => [-side => "left", -anchor => "w"],
			 -width        => 20,
			   -numberonly   => '1',
			 -textvariable => \$self->{p},
			)->pack(
				-side   => 'top',
				-anchor => 'nw',
				-fill   => 'x',
				-padx   => 10,
				-pady   => 2,
			       );
    my $q_entry =
      $self->{q_entry} = 
	$frame->EuLabEntry(
			 -label        => sprintf("%-20s%5s%s","material q","[ ]","*"),
			 -labelPack    => [-side => "left", -anchor => "w"],
			 -width        => 20,
			   -numberonly   => '1',
			 -textvariable => \$self->{q},
			)->pack(
				-side   => 'top',
				-anchor => 'nw',
				-fill   => 'x',
				-padx   => 10,
				-pady   => 2,
			       );
    my $calc =
      $self->{B_CALC}
	= $frame->Button(
			 -text             => 'CS now!',
			 -command          => sub 
			 {
			   my $file = $self->{cppfile};
			   unless (-e $file)
			     {
			       $self->{ERROR} = 'File does not exist! Select a valid *.fai File!';
			       $self->inform_about_data;
			       return;
			     }

			   $self->generate_cs_factor;
			   my $eq_stress = $self->{EQ_STRESS};
			   my $cs_factor = $self->{CALCULATED_CS_FACTOR};
			   my $dsg       = $self->{DSG};
			   $self->{INFO} = sprintf("%8s %s %5.2f %3s %s %4.2f [%7s %s%10i]", "EQ-VALUE","\=","$eq_stress","CS","\=","$cs_factor","Valid-N","\=","$dsg");
			  #$self->{INFO} = "< EQ stress \= $eq_stress > < CS \= $cs_factor ($dsg DSG) >";
			 }
			)->pack(
				-side   => 'left',
				-anchor => 'e',
				-padx   => 2,
				-pady   => 2,
			       );
    my $exit  =
      $self->{B_CANCEL}
	= $frame->Button(
			 -text             => 'Cancel',
			 -command          => sub 
			 {
			   $top->destroy() if Tk::Exists($top);
			  #delete $top;
			 }
			)->pack(
				-side   => 'right',
				-anchor => 'e',
				-padx   => 2,
				-pady   => 2,
			       );

    my $label = $frame->Label(
			      -textvariable =>\$self->{INFO},
			     #-width        => 80,
			     #-font         => 'Courier 8 bold',
			     #-relief       => 'groove',
			      -foreground   => 'blue',
			     #-takefocus    => 1
			     )->pack(
				     -side => 'top',
				     -fill => 'x',
				     -padx => 10, 
				    );
   # $top->grabGlobal;
  }



sub inform_about_data
  {
    my $self = shift;
    my $root      = $self->{ROOT};
    my $gui       = $root->{GUI};

    my $ok = 'OK';
    my $dialog = $gui->Dialog( 
			      -title          => 'Information',
			      -text           => '',
			      -bitmap         => 'info',
			      -default_button => $ok,
			      -buttons        => [$ok],
			     );

    $dialog->configure(
		       -wraplength => '8i',
		       -text       => "$self->{ERROR}",
		      );

    my $button = $dialog->Show('-global');
  }


sub generate_cs_factor
  {
    my $self        = shift;
    $self->{INPUT}  = $self->{FILE};
    $self->{TEMP}   = $self->{INPUT} . '.tmp';
    $self->{OUTPUT} = $self->{INPUT} . '.csf';

    open( INPUT, "< $self->{INPUT}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    $self->{DIRECTIONS} = \@contents;

    $self->acertain_whether_spectra_or_rain_or_pure($self);
#   $self->activate_objscan;
  }



sub acertain_whether_spectra_or_rain_or_pure
  {
    my $self   = shift;
    my $fa_1   = 'N';
    my $fa_2   = 'N';
    my $fa_3   = 'N';

    open( INPUT,  "< $self->{cppfile}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    foreach (@contents)
      {
        my $line      = $_;

	if ($line =~m/^\s*Begin Rainflow_Stress_Spectrum\s*$/) 
	  {
	    $fa_1 = 'R';
	  }
      }

    foreach (@contents)
      {
        my $line      = $_;

	if ($line =~m/begin STRESS_SPECTRUM/)
	  {
	    $fa_2 = 'S';
	  }
      }

    foreach (@contents)
      {
        my $line      = $_;

	if ($line =~m/^\s*begin\s*Loads/)
	  {
	    $fa_3 = 'P';
	  }
      }

    if (($fa_1 =~m/N/) && ($fa_2 =~m/N/) && ($fa_3 =~m/P/))
      {
	print "  * CsFactorPure switch ...";
	$self->calculate_pure_cs_factor;
	print "  Cs derived!\n";
      }

    if (($fa_1 =~m/N/) && ($fa_2 =~m/S/))
      {
	print "  * Using Spectra-switch only\n";
	$self->convert_exceed_to_occurence; # convert exceedence to occurence
	$self->load_cpp_stress_spectra;     # get cpp-stress spectra
	$self->calculate_cs_factor;
      }

    if (($fa_1 =~m/R/) && ($fa_2 =~m/N/))
      {
	print "  * Using Rainflow-switch only\n";
	$self->calculate_rain_cs_factor;
      }

    if (($fa_1 =~m/R/) && ($fa_2 =~m/S/))
      {
	$self->{ERROR} = 'You cannot have Spectra and Rainflow together! Job Rejected!';
	$self->inform_about_data;
	return;
      }
  }




sub calculate_pure_cs_factor
  {
    my $self   = shift;
    my $type   = 'N';
    my $root      = $self->{ROOT};
    my $dsg    = $self->{DSG} = $root->{validity};
    my $sum    = 0.0;
    my $damage = 0.0;

    open( INPUT,  "< $self->{cppfile}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    my $stress_reference = $self->{stress_reference};
    my $IQF              = $self->{IQF};
    my $factor           = $self->{factor};
    my $P                = $self->{p};
    my $Q                = $self->{q};

    open( OUTPUT, "> $self->{OUTPUT}" );
    &write_input2output($self);

    foreach (@contents)
      {
        my $line      = $_;
	   $line      =~s/^\s*//;

	if ($line eq '')
	  {
	    next;
	  }

	if ($line =~m/^\s*begin\s*Loads/)
	  {
	    $type = 'R';
	    next;
	  }
        elsif ($line =~m/^\s*end\s*Loads/)
	  {
	    $type = 'N';
	  }

	if ($type =~m/R/) 
	  {
	    $line =~s/^\s*//;

	    my ($cycle, $max, $min) = split('\=', $line);

	    if ($max <= 0) 
	      {
		$max = 0.1;
		$min = 0.1;
	      }

	    if ($max < $min) 
	      {
		$max = $max;
		$min = $max;
	      }

	    my $r     = $min / $max;

	    if ($r == 1.00)
	      {
		$r = 0.99;
	      }

	    # test
	    my $value = ((1.0 - $r)/0.9) ** $Q;

	    if ($value =~m/\?/) 
	      {
		print OUTPUT " *** something went wrong!\n";
	      }
	    # end test

	    my $sum_i = (((1.0 - $r)/0.9)**$Q * $max)** $P;
	    my $dam_i = 1.0 / (1.0e5 * ($IQF / ((1.0 - $r) / 0.9)**$Q / $max)**$P) * $cycle;

	    $sum      = $sum    + $sum_i;
	    $damage   = $damage + $dam_i;

	    print OUTPUT sprintf("%10i %10.2f %10.2f %10.5f  %20.19f %s", "$cycle", "$max", "$min", "$r", "$dam_i", "\n");
	  }
      }

    my $total_damage = $damage;
    #   my $equiv_stress = $IQF * (10.0*$damage)**(1.0/$P);  #OLD
    my $equiv_stress = $IQF * (1.0E5/$dsg*$damage)**(1.0/$P);
    my $cs_factor    = $equiv_stress / $stress_reference;
    my $fatigue_life = 1.0e5 * ($IQF / $equiv_stress)** $P;
    $self->{CALCULATED_CS_FACTOR} = $cs_factor;
    $self->{EQ_STRESS}            = $equiv_stress;

    print OUTPUT "\n\n";
    print OUTPUT  sprintf("%20s %30.2f %s",  "Safe_Reference     \=",  "$sum",         "\n");
    print OUTPUT  sprintf("%20s %30.19f %s", "Total_Damage       \=",  "$damage",      "\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "Equivalent_Stress  \=",  "$equiv_stress","\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "Fatique_Life       \=",  "$fatigue_life","\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "CS_Factor          \=",  "$cs_factor",   "\n");

    close(OUTPUT);
  }



sub calculate_rain_cs_factor
  {
    my $self   = shift;
    my $type   = 'N';
    my $root      = $self->{ROOT};
    my $dsg    = $self->{DSG} = $root->{validity};

    my $sum    = 0.0;
    my $damage = 0.0;

    open( INPUT,  "< $self->{cppfile}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    my $stress_reference = $self->{stress_reference};
    my $IQF              = $self->{IQF};
    my $factor           = $self->{factor};
    my $P                = $self->{p};
    my $Q                = $self->{q};

    open( OUTPUT, "> $self->{OUTPUT}" );
    &write_input2output($self);

    foreach (@contents)
      {
        my $line      = $_;
	   $line      =~s/^\s*//;

	if ($line eq '')
	  {
	    next;
	  }

	if ($line =~m/^\s*Begin Rainflow_Stress_Spectrum\s*$/) 
	  {
	    $type = 'B';
	    next;
	  }

	if (($line =~m/^\s*\{\s*$/) && ($type =~m/B/))
	  {
	    $type = 'R';
	    next;
	  }
        elsif (($line =~m/^\s*\}\s*$/) && ($type =~m/R/))
	  {
	    $type = 'N';
	  }

	if ($type =~m/R/) 
	  {
	    my ($cycle, $max, $min) = split('\s+', $line);

	    if ($max <= 0) 
	      {
		$max = 0.1;
		$min = 0.1;
	      }

	    if ($max < $min) 
	      {
		$max = $max;
		$min = $max;
	      }

	    my $r     = $min / $max;

	    if ($r == 1.00)
	      {
		$r = 0.99;
	      }

	    # test
	    my $value = ((1.0 - $r)/0.9) ** $Q;

	    if ($value =~m/\?/) 
	      {
		print OUTPUT " *** something went wrong!\n";
	      }
	    # end test

	    my $sum_i = (((1.0 - $r)/0.9)**$Q * $max)** $P;
	    my $dam_i = 1.0 / (1.0e5 * ($IQF / ((1.0 - $r) / 0.9)**$Q / $max)**$P) * $cycle;

	    $sum      = $sum    + $sum_i;
	    $damage   = $damage + $dam_i;

	    print OUTPUT sprintf("%10i %10.2f %10.2f %10.5f  %20.19f %s", "$cycle", "$max", "$min", "$r", "$dam_i", "\n");
	  }
      }

    my $total_damage = $damage;
    my $equiv_stress = $IQF * (1.0E5/$dsg*$damage)**(1.0/$P);
    my $cs_factor    = $equiv_stress / $stress_reference;
    my $fatigue_life = 1.0e5 * ($IQF / $equiv_stress)** $P;
    $self->{CALCULATED_CS_FACTOR} = $cs_factor;
    $self->{EQ_STRESS}            = $equiv_stress;

    print OUTPUT "\n\n";
    print OUTPUT  sprintf("%20s %30.2f %s",  "Safe_Reference     \=",  "$sum",         "\n");
    print OUTPUT  sprintf("%20s %30.19f %s", "Total_Damage       \=",  "$damage",      "\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "Equivalent_Stress  \=",  "$equiv_stress","\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "Fatique_Life       \=",  "$fatigue_life","\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "CS_Factor          \=",  "$cs_factor",   "\n");

    close(OUTPUT);
  }



sub write_input2output
  {
    my $self   = shift;

    print OUTPUT "\nUSER INPUT DATA: \n";
    print OUTPUT "begin CS_FACTOR\n";
    print OUTPUT "  cppfile          \= $self->{cppfile}\n";
    print OUTPUT "  stress_reference \= $self->{stress_reference}\n";
    print OUTPUT "  IQF              \= $self->{IQF}\n";
    print OUTPUT "  factor           \= $self->{factor}\n";
    print OUTPUT "  p                \= $self->{p}\n";
    print OUTPUT "  q                \= $self->{q}\n";
    print OUTPUT "end CS_FACTOR\n";
    print OUTPUT "\n\n\nCALCUALTION DATA: \n";
  }



sub calculate_cs_factor
  {
    my $self   = shift;
    my $sum    = 0.0;
    my $damage = 0.0;
    my $root      = $self->{ROOT};
    my $dsg    = $self->{DSG} = $root->{validity};

    my @segnames         = @{$self->{SEGNAMES}};
    my $stress_reference = $self->{stress_reference};
    my $IQF              = $self->{IQF};
    my $factor           = $self->{factor};
    my $P                = $self->{p};
    my $Q                = $self->{q};

    open( OUTPUT, "> $self->{OUTPUT}" );
    $self->write_input2output;

    foreach (@segnames) 
      {
	my $segment = $_;
	print OUTPUT "\n$segment\n";

	for (1 .. $self->{$segment}->{NUM}) 
	  {
	    my $i     = $_ - 1;

	    my $cycle = $self->{$segment}->{CYCLES}[$i];
	    my $max   = $self->{$segment}->{S_US}[$i] * $factor;
	    my $min   = $self->{$segment}->{S_LS}[$i] * $factor;

	    if ($max <= 0) 
	      {
		$max = 0.1;
		$min = 0.1;
	      }

	    if ($max < $min) 
	      {
		$max = $max;
		$min = $max;
	      }

	    my $r     = $min / $max;

	    if ($r == 1.00)
	      {
		$r = 0.99;
	      }

	    # test
	    my $value = ((1.0 - $r)/0.9) ** $Q;

	    if ($value =~m/\?/) 
	      {
		print OUTPUT "  ***  Something went wrong!\n";
		print STDERR "  ***  Something went wrong!\n";
	      }
	    # end test

	    my $sum_i = (((1.0 - $r)/0.9)**$Q * $max)** $P;
	    my $dam_i = 1.0 / (1.0e5 * ($IQF / ((1.0 - $r) / 0.9)**$Q / $max)**$P) * $cycle;

	    $sum      = $sum    + $sum_i;
	    $damage   = $damage + $dam_i;

	    print OUTPUT sprintf("%10i %10.2f %10.2f %10.5f  %20.19f %s", "$cycle", "$max", "$min", "$r", "$dam_i", "\n");
	  }
      }

    my $total_damage = $damage;
#   my $equiv_stress = $sum ** (1.0/$P); # OLD METHOD
#   my $equiv_stress = $IQF * (10.0*$damage)**(1.0/$P);  # OLD

    my $equiv_stress = $IQF * (1.0E5/$dsg*$damage)**(1.0/$P);
    my $cs_factor    = $equiv_stress / $stress_reference;
    my $fatigue_life = 1.0e5 * ($IQF / $equiv_stress)** $P;
    $self->{CALCULATED_CS_FACTOR} = $cs_factor;
    $self->{EQ_STRESS}            = $equiv_stress;

    print OUTPUT "\n\n";
    print OUTPUT  sprintf("%20s %30.2f %s",  "Safe_Reference     \=",  "$sum",         "\n");
    print OUTPUT  sprintf("%20s %30.19f %s", "Total_Damage       \=",  "$damage",      "\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "Equivalent_Stress  \=",  "$equiv_stress","\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "Fatique_Life       \=",  "$fatigue_life","\n");
    print OUTPUT  sprintf("%20s %30.2f %s",  "CS_Factor          \=",  "$cs_factor",   "\n");

    close(OUTPUT);
  }


sub convert_exceed_to_occurence
  {
    my $self = shift;
    my $i    =  0 ;

    open( INPUT,  "< $self->{cppfile}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    open( OUTPUT, "> $self->{TEMP}" );
    my $flag      = 'N';
    my $b         = 'N';

    foreach (@contents)
      {
        my $line      = $contents[$i];
	my $next_line = $contents[$i+1];
	
	if ($line =~m/^\s*SEGNAME\s*\=\s*GAG\s*$/)
	  {
	    $flag   = 'Z';
	    print "  * GAG Found\n";
	  }
	
        if ((defined $next_line) && ($next_line =~m/^\s*\}\s*$/))
	  {
	    $flag = 'N';
	    $b    = 'S';
	  }
	
        if ($line =~m/^\s*end\s*$/)
	  {
	    $flag = 'N';
	    $b    = 'N';
	  }
	
        if (($line =~m/begin STRESS_SPECTRUM/) && ($flag !~m/L/) && ($flag !~m/P/))
	  {
	    $flag = 'Y';
	    print OUTPUT "$line\n"; 
	  }
	elsif (($flag !~m/L/) && ($b !~m/S/) && ($flag !~m/P/))
	  {
	    print OUTPUT "$line\n"; 
	  }
	
	$b         = 'N';

	if ($flag =~m/L/)
	  {
	    $line         =~s/^\s*//;
	    $next_line    =~s/^\s*//;
	    my ($cyc_1, $s_us_1, $s_ls_1, $s_ampl_1, $r_s_1) = split('\s+', $line);
	    my ($cyc_2, $s_us_2, $s_ls_2, $s_ampl_2, $r_s_2) = split('\s+', $next_line);
	    
	    my $cyc  = $cyc_2  - $cyc_1;
	    my $s_us = ($s_us_2 + $s_us_1) / 2.0;
	    my $s_ls = ($s_ls_2 + $s_ls_1) / 2.0;
	    
	   #print OUTPUT        "                       $cyc   $s_us   $s_ls   $i    $i \n";
	    print OUTPUT sprintf("%30i %10.4f  %10.4f  %10i %10i %s","$cyc","$s_us","$s_ls","$i","$i","\n");
	  }
	
	if ($flag =~m/P/)
	  {
	    $line         =~s/^\s*//;
	    $next_line    =~s/^\s*//;
	    my ($cya_1, $s_ua_1, $s_la_1, $n_ua_1, $n_la_1, $s_aa_1, $r_a_1) = split('\s+', $line);
	    my ($cya_2, $s_ua_2, $s_la_2, $n_ua_2, $n_la_2, $s_aa_2, $r_a_2) = split('\s+', $next_line);
	    
	    my $cya  = $cya_2  - $cya_1;
	    my $s_ua = ($s_ua_2 + $s_ua_1) / 2.0;
	    my $s_la = ($s_la_2 + $s_la_1) / 2.0;
	    
	   #print OUTPUT "                       $cya   $s_ua   $s_la   $i    $i   $i   $i\n";
	    print OUTPUT sprintf("%30i %10.4f  %10.4f  %10i %10i %10i %10i %s", "$cya","$s_ua","$s_la","$i","$i","$i","$i","\n");
	  }
	
        if (($line =~m/^\s*\{\s*$/) && ($flag =~m/Y/))
	  {
	    $flag = 'L';
	  }
        elsif (($line =~m/^\s*\}\s*$/) && ($flag =~m/Y/))
	  {
	    print OUTPUT "$line\n"; 
	  }
	
        if (($line =~m/^\s*\{\s*$/) && ($flag =~m/Z/))
	  {
	    $flag = 'P';
	  }
        elsif (($line =~m/^\s*\}\s*$/) && ($flag =~m/Z/))
	  {
	    $flag = 'N';
	    print OUTPUT "$line\n"; 
	  }
	
	
	$i++;
      } 
    close(OUTPUT);
  }


sub load_cpp_stress_spectra
  {
    my $self = shift;
    my $type = 'N';
    my $i    =  0 ;
    my $seg  = 'NONE';
    my @segnames;

    open( INPUT, "< $self->{TEMP}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    # modify contents
    my @temp;
    foreach (@contents) 
      {	
	my $flag = 'N';
	my $segment = 'NONE';

	my $line = $_;

	if ($_ =~m/^\s*SEGNAME/)
	  {	
	    $i++;
	    my ($key_s, $value_s) = split('\s*=\s*', $_);
	    $key_s                     =~s/\s*//;
	    $value_s                   =~s/\s*//;
	    $segment                   = $value_s;
	    $line                      = '  SEGNAME      = ' . $segment . $i;
	  }
	push(@temp, $line);	
      }
    @contents = @temp;
    # end modify

    $i = 0;

    $self->{CPP_CONTENTS} = \@contents;


    foreach (@contents)
      {
	if ($_ eq "") 
	  {
	    next; 
	  }

	if ($_ =~m/begin STRESS_SPECTRUM/)
	  {
	    $type = 'Y';
	    $i    = 0;
	    next;
	  }
	if ($_ =~m/end/)
	  {
	    $type = 'N';
	    next;
	  }

	if (($type =~m/Y/) && ($_ =~m/SEGNAME/))
	  {	
	    my ($key, $value) = split('\s*=\s*', $_);
	    $key                     =~s/\s*//;
	    $seg                     = $key;
	    $self->{$key}            = $value;
	    push(@segnames, $self->{$key});

	    if ($self->{$key} =~m/GAG/) 
	      {
		$type = 'S';
	      }
	  }

	if (($type =~m/Y/) && ($_ =~m/\{/))
	  {	
	    $type = 'L';
	    next;
	  }

	if (($type =~m/S/) && ($_ =~m/\{/))
	  {
	    $type = 'G';
	    next;
	  }

      	if ((($type =~m/L/) || ($type =~m/G/)) && ($_ =~m/\}/))
	  {	
	    $type = 'N';
	    next;
	  }


	if ($type =~m/L/)
	  {
	    my $line = $_;
	    $line    =~s/^\s*//;
	    my ($cyc, $s_us, $s_ls, $s_ampl, $r_s) = split('\s+', $line);

	    $self->{"$self->{$seg}"}->{CYCLES}[$i] = $cyc;
	    $self->{"$self->{$seg}"}->{S_US}[$i]   = $s_us;
	    $self->{"$self->{$seg}"}->{S_LS}[$i]   = $s_ls;
	    $self->{"$self->{$seg}"}->{S_AMPL}[$i] = $s_ampl;
	    $self->{"$self->{$seg}"}->{R_S}[$i]    = $r_s;
	    $self->{"$self->{$seg}"}->{NUM}        = $i + 1;
	    $i++;
	  }

	if ($type =~m/G/)
	  {
	    my $line = $_;
	    $line    =~s/^\s*//;
	    my ($cyc, $s_us, $s_ls, $n_us, $n_ls, $s_as, $r_s) = split('\s+', $line);

	    $self->{"$self->{$seg}"}->{CYCLES}[$i] = $cyc;
	    $self->{"$self->{$seg}"}->{S_US}[$i]   = $s_us;
	    $self->{"$self->{$seg}"}->{S_LS}[$i]   = $s_ls;
	    $self->{"$self->{$seg}"}->{S_AS}[$i]   = $s_as;
	    $self->{"$self->{$seg}"}->{R_S} [$i]   = $r_s;
	    $self->{"$self->{$seg}"}->{NUM}        = $i + 1;
	    $i++;
	  }
      }
    $self->{SEGNAMES} = \@segnames;
  }



sub fileDialog 
  {
    my $self  = shift;
    my $root  = $self->{ROOT};

    my $frame = $root->{MW};
    my $operation = $self->{OPERATION};
    my @types;

    #   Type names		Extension(s)	Mac File Type(s)
    #
    #---------------------------------------------------------
    @types =
      (
       ["*.fai files",		'.fai'],
       ["All files",		'*']
      );

    my $File = $self->{FILE};

    if ($operation =~m/open/)
      {
	$self->{FILE} = $frame->getOpenFile(
					    -filetypes => \@types
					   );
      }
    else
      {
	$self->{FILE} = $frame->getSaveFile(
					    -filetypes => \@types,
					    -initialfile => 'CS',
					    -defaultextension => '.csin'
					   );
      }
  }



### routines below are no longer required ### 


sub load_user_information
  {
    my $self     = shift;

    my @contents = @{$self->{DIRECTIONS}};
    my $type     = 'N';

    foreach (@contents)
      {
	if ($_ eq "") 
	  {
	    next; 
	  }

	if ($_ =~m/begin CS_FACTOR/)
	  {
	    $type = 'Y';
	    next;
	  }
	if ($_ =~m/end CS_FACTOR/)
	  {
	    next;
	  }

	if ($type =~m/Y/)
	  {	
	    my ($key, $value) = split('\s*=\s*', $_);
	    if((defined $key) && (defined $value))
	      {
		$key                     =~s/\s*//;
		$self->{$key}            = $value;
	      }
	  }
      }
  }



sub write_input2output_old
  {
    my $self   = shift;

    print OUTPUT "\nUSER INPUT DATA: \n";

    foreach (@{$self->{DIRECTIONS}}) 
      {
	unless ($_ eq '') 
	  {
	    print OUTPUT "$_\n"; 
	  }
      }

    print OUTPUT "\n\n\nCALCUALTION DATA: \n";
  }



sub check_validity_of_data
  {
    my $self     = shift;
    my @contents = @_;
    my $flag     = 1;

    foreach (@contents) 
      {
	my $line = $_;

	unless (($line =~m/^[\d.\-\+]+$/) || ($line =~m/^\s*\n$/))
	  {
	    $flag++;
	  }
      }

    if ($flag > 1) 
      {
	$self->{USE_DATA} = 'NO';
	$self->{ERROR}    = 'DATIG thinks your data is Un-Useable! You should have only Numbers!';
	$self->inform_about_data;		
      }
  }



sub activate_objscan
  {
    my $self  = shift;

    my $mw = MainWindow->new();

    $mw->title("Data and Object Scanner");

    my $scanner   = $mw->ObjScanner(
				    caller => $self
				   )->pack(
					   -expand => 1
					  );
  }



1;



