package Tk::PLOT_MAXMEANMIN_FSB_040;

use strict;
use POSIX qw(acos sin cos ceil floor log10 atan);   
use Tk;                                             
require Tk::BrowseEntry;
#use Tk::ObjScanner;
#my $self  = {};

#Derived from Tool = Plot_MaxMeanMin_Extreme_FlightSegmentBlock_MultiMore_v4.0.pl
#This program Plots the Extremes MaxMeanMin Segments of a Mission.  Input is: *.profile file

#print GLOBAL_LOG "##   This program Plots the Extremes MaxMeanMin Segments of a Mission     \n";
#print GLOBAL_LOG "## --------------->>> Input is: *.profile file <<<--------------- \n";
#print GLOBAL_LOG "## ----->>> .......... AutoMulti-Step Version ................... \n";
#print GLOBAL_LOG "## ----->>> ............ with Sel. 1g only .......................\n";
#print GLOBAL_LOG "## ----->>> .... from v3.6 you can Compare 2 plots Together! .....\n";
#print GLOBAL_LOG "## ----->>> .... from v4.0 implemented the Mouse Over Info!  .....\n";

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
	$self->{tool_version}  = '4.0';
	
    if (@_)
      {
        my %extra = @_;
        @$self{keys %extra} = values %extra;
      }

    my $root                          = $self->{ROOT};
	my $wmai                          = $self->{MY_MISSION};
	my $plot_what                     = $self->{PLOT};        # STH or PLOT  where PLOT is Deltap!	
	
	open(GLOBAL_LOG, ">>". $root->{GLOBAL}->{GLOBAL_LOGFILE}); 	
	$self->{directory_path}           = $root->{GLOBAL}->{WORKING_DIRECTORY};    
    $self->{date}                     = $root->{GLOBAL}->{DATE};
	$self->{mission}                  = $root->{MISSION}->{$wmai}->{MISSION};
	
	$self->{show_text_more}           = $root->{PLOT_OPTIONS}->{show_text_more};
	$self->{do_fixed_scale_y}         = $root->{PLOT_OPTIONS}->{do_fixed_scale_y};
	$self->{force_scale_y_to}         = $root->{PLOT_OPTIONS}->{force_scale_y_to};
	$self->{neutral_axis_y_shift}     = $root->{PLOT_OPTIONS}->{neutral_axis_y_shift};       # 410   290 orig  330 370 410 (steps of 40)
	
    $self->{error}                    = 0;	
	print GLOBAL_LOG "  * |$self->{directory_path}|\n";
	$self->{color_for_2_now}          = 'green'; # Initialize!
	
	$self->{eid}                      = $root->{MISSION}->{$wmai}->{STF_FILE};
	if ($self->{eid} =~m/\..+$/i) {$self->{eid} =~s/\..+//;} else {$self->{eid} = $self->{eid} . '_x';}	
	$self->{PROFILE_FILE}             = $self->{eid} .  '_HQ.profile';

    print GLOBAL_LOG "\n  * <<START>> PLOT PROFILE MaxMeanMin Extreme Flight Segment Block Process  |$wmai|$self->{PROFILE_FILE}|\n\n";
	unless (-e $self->{PROFILE_FILE})  {print GLOBAL_LOG " *** Could not find PROFILE File |$self->{PROFILE_FILE}|!\n"; return;}
    $self->Start_PLOT_PROFILE_MAXMEANMIN_process($root);
	print GLOBAL_LOG "\n  * <<END>>   Completed Plot Process\n";
	close(GLOBAL_LOG);
  }


sub Start_PLOT_PROFILE_MAXMEANMIN_process
   {
     my $self = shift;
	 my $root = shift;   
     ##### PLOT IniTiaTor #####
     &build_Tk_mainframe_objects($self);
     &build_canvas_object_only($self);
     unless ($self->{PROFILE_FILE} =~m/\.profile/) { &welcome_initialize($self); }
	 else   
	   {
	      &sort_out_the_data_with_reference_to_selected_options($self);
		  my @levels = ('1','2','3','4','5');
		  foreach(@levels)
		     {
			  $self->{upto_incre_level} = $_;
			  foreach (@{$self->{missions_found}})
			     {
			 	   $self->{mission}      = $_; print GLOBAL_LOG "        Plotting_Profile_Mission $_     \n";
                   &sort_out_the_data_with_reference_to_selected_options($self);
			       $self->{MENU}->cget(-menu)->invoke('Gen PostScript');        
			     }
			 }
	   }
     #MainLoop;		   #  ACTIVATE this LINE to see the TK Object Plotted!
     $self->{MW}->destroy() if Tk::Exists($self->{MW});	
   }



sub sort_out_the_data_with_reference_to_selected_options
   {
     my $self = shift;
	 unless (defined $self->{added_file}) {$self->{added_file} = 1;}  # $self->{added_file} is Num of Files to compare ! This is needed only for Multiple files
	 my $num  = $self->{added_file};   
	 
	 return unless (defined $self->{PROFILE_FILE});
	 
	 &load_user_selected_data_file_baseline($self,);   #print GLOBAL_LOG "  * File Loaded\n";
	 #print GLOBAL_LOG " ERR:  $self->{error} | @{$self->{steady_codes_found}}[1] | $self->{PROFILE_FILE}\n";
	 if ($self->{error} < 1) 
	   {
         unless ($self->{steady_code_selected} =~m/ALL/i) {$self->{plot_only_1g_segment} = 'YES';  @{$self->{plot_this_1g}} = $self->{steady_code_selected};}
	     if (defined @{$self->{steady_codes_found}}[0]) {$self->{segments_menu}->configure( -choices => \@{$self->{steady_codes_found}}, );}
         if (defined @{$self->{missions_found}}[0]) {$self->{mission_menu}->configure( -choices  => \@{$self->{missions_found}}, );}
		 $self->{up_down_menu}->configure( -choices  => \@{$self->{array_plot_PN_side}},);
	     return unless (defined @{$self->{steady_codes_found}}[1]);
         &build_canvas_object_only($self); 
         #print GLOBAL_LOG "  * File Loaded $self->{error} \n";
		 if ($num  == 1)
		   {		 
             &plot_organized_data_on_Tk_canvas_single($self,);
		   }
		 if ($num  > 1)
		   {
		     &load_user_selected_data_file_otherfile($self,);
		     &plot_organized_data_on_Tk_canvas_multiple($self,);
		   }
       } 
	 else {&welcome_initialize($self);}	 
   }

 

  
sub load_user_selected_data_file_otherfile
   {
     my $self    = shift;
	 #my $filenum = shift;
     my $file = $self->{PROFILE_FILE_2}; 
	 
     open(INPUT, "<".$file) or  return;
     my @data = <INPUT>;
     chop(@data);
     close(INPUT);
	 @{$self->{all_profile_file_contents_2}} = @data;
	 
	 #$self->{plot_only_1g_segment}    = 'NO';
     @{$self->{steady_codes_found_2}} =  ('ALL');
     @{$self->{missions_found_2}}     = ();
    foreach (@{$self->{all_profile_file_contents_2}})
	  {
	    if (($_  =~m/^\s*$/) || ($_ =~m/\#/))    { next;}
	    my $line = $_; $line =~s/^\s*//;
		my @values = split('\s+', $line);
		my $name   = shift(@values);
		my $code   = shift(@values);  #print GLOBAL_LOG "$code\n";
		my $length = length($code);
		my $mn    = unpack('A1 A4', $code);
        $self->{missions_num_available_2}->{$mn} = 1;	
	    push(@{$self->{steady_codes_found_2}}, $code) if ($mn =~m/$self->{mission}/);
		unless($length == 5) {$self->{error}++; print GLOBAL_LOG "   *** |$code| not expected!\n";} #
	  }	
     foreach (sort keys %{$self->{missions_num_available_2}}) { push (@{$self->{missions_found_2}}, $_); }
	 } # end load file

   
   
sub load_user_selected_data_file_baseline
   {
     my $self    = shift;
	 #my $filenum = shift;
     my $file = $self->{PROFILE_FILE}; 
	 
     open(INPUT, "<".$file) or  return;
     my @data = <INPUT>;
     chop(@data);
     close(INPUT);
	 @{$self->{all_profile_file_contents}} = @data;
	 
	 $self->{plot_only_1g_segment}  = 'NO';
     @{$self->{steady_codes_found}} =  ('ALL');
     @{$self->{missions_found}}     = ();
    foreach (@{$self->{all_profile_file_contents}})
	  {
	    if (($_  =~m/^\s*$/) || ($_ =~m/\#/))    { next;}
	    my $line = $_; $line =~s/^\s*//;
		my @values = split('\s+', $line);
		my $name   = shift(@values);
		my $code   = shift(@values);  #print GLOBAL_LOG "$code\n";
		my $length = length($code);
		my $mn    = unpack('A1 A4', $code);
        $self->{missions_num_available}->{$mn} = 1;	
	    push(@{$self->{steady_codes_found}}, $code) if ($mn =~m/$self->{mission}/);
		unless($length == 5) {$self->{error}++; print GLOBAL_LOG "   *** |$code| not expected!\n";} #
	  }	
     foreach (sort keys %{$self->{missions_num_available}}){push (@{$self->{missions_found}}, $_);} 
	 } # end load file


	 
sub plot_organized_data_on_Tk_canvas_multiple
  {
    my $self          = shift;
    my $file          = $self->{PROFILE_FILE};
    my $file_2        = $self->{PROFILE_FILE_2};
	my $canvas        = $self->{CANVAS};
    my @colour_upper  = qw/blue blue green Coral DarkViolet Goldenrod brown Orchid/;      #my @colour_upper = qw/Blue Blue LawnGreen DarkOrchid1 Orange DeepPink/;
    my @colour_lower  = qw/red red gold cyan pink purple LightCyan Magenta/;	          #my @colour_lower = qw/Red Red OliveDrab1 Gold Magenta Orchid DarkViolet/;
    my $colour_mean   = 'black';
	my $colour_mean_2 = 'Goldenrod';
    my $colour_dp     = 'purple';
	my $colour_dp_2   = 'Goldenrod';
	my $colour_D1     = 'blue';
	my $colour_D2     = $self->{color_for_2_now}; #'green';

    my @material_colours = qw/yellow red blue green black cyan purple orange pink gold grey brown violet white/;	
    my @plotdata         = @{$self->{all_profile_file_contents}};   #print GLOBAL_LOG "@plotdata\n";
    my @plotdata_2       = @{$self->{all_profile_file_contents_2}}; #print GLOBAL_LOG "@plotdata\n";	
    ##### Find maximum DATA values
    my $max_y   = 0.0;
    my $max_x   = 0;
    my $num     = 0;
    my $mission = $self->{mission};
    @{$self->{ALL_CODES}}   = ();
    @{$self->{ALL_CODES_2}} = ();	
    foreach (@plotdata)
      {
	    if (($_  =~m/^\s*$/) || ($_ =~m/\#/))    { next;}
	    my $line = $_; $line =~s/^\s*//;
		my @values = split('\s+', $line);
		my $name   = shift(@values);
		my $code   = shift(@values);
		my $min    = shift(@values);
		my $mean   = shift(@values);
		my $max    = shift(@values);
		my $dp     = shift(@values);  #print GLOBAL_LOG "  * $name \n";
		if ($line =~m/CONSTANT/) {$self->{STEP_CONSTANT}->{$code} = pop(@values); $self->{STEP_COMMENT}->{$code} = pop(@values);}
	    if (defined $self->{STEP_COMMENT}->{$code}) {my $const = $self->{STEP_CONSTANT}->{$code};  $mean = $mean + $const;}
		
		for (1 .. 99)
		  {
		    unless (defined $values[3]) {last;}
		    my $step = shift(@values);   # 1-8
			my $pos  = shift(@values);   # 1-5
			$self->{STEP}->{$code}->{$step}->{$pos}->{min}  = shift(@values);
			$self->{STEP}->{$code}->{$step}->{$pos}->{mean} = shift(@values);
			$self->{STEP}->{$code}->{$step}->{$pos}->{max}  = shift(@values);
			
		    if (defined $self->{STEP_COMMENT}->{$code}) 
		      {
			    my $const = $self->{STEP_CONSTANT}->{$code};
			    if ($pos == 1) 
				  {
				    $self->{STEP}->{$code}->{$step}->{$pos}->{mean} = $self->{STEP}->{$code}->{$step}->{$pos}->{mean} + $const; 
				    $self->{STEP}->{$code}->{$step}->{$pos}->{max}  = $self->{STEP}->{$code}->{$step}->{$pos}->{max}  + $const; 
					$self->{STEP}->{$code}->{$step}->{$pos}->{min}  = $self->{STEP}->{$code}->{$step}->{$pos}->{min}  + $const;
				  }
			    else {$self->{STEP}->{$code}->{$step}->{$pos}->{mean} = $self->{STEP}->{$code}->{$step}->{$pos}->{mean} + $const;}
			  }				
		  }

		my $mn    = unpack('A1 A4', $code);
		next unless ($mn =~m/$mission/);
		
	    if((defined $min) && (defined $max))
	      {
	        $code  =~s/\s*//;
	        $self->{DATA}->{$code}->{name} = $name;
		    $self->{DATA}->{$code}->{max}  = $max;
		    $self->{DATA}->{$code}->{min}  = $min;
		    $self->{DATA}->{$code}->{mean} = $mean;
		    $self->{DATA}->{$code}->{dp}   = $dp;
	        if ($max  >  $max_y)
		     {
		       $max_y  = $max;          #Y-axis
			   $self->{neutral_axis_y_shift} = 290;
		     }
			if (abs($min) > $max_y) {$max_y = abs($min); $self->{neutral_axis_y_shift} = 170;}
			 
		    $num++;   
	        if ($num >  $max_x)
		     {
		       $max_x  = $num;    #X-axis
		     }
		    push(@{$self->{ALL_CODES}}, $code);   
	      }
	  }
	$self->{total_segments_for_plotting} = $num;  # Segments are based on Baseline Pilot Point!
	
    foreach (@plotdata_2)
      {
	    if (($_  =~m/^\s*$/) || ($_ =~m/\#/))    { next;}
	    my $line = $_; $line =~s/^\s*//;
		my @values = split('\s+', $line);
		my $name   = shift(@values);
		my $code   = shift(@values);
		my $min    = shift(@values);
		my $mean   = shift(@values);
		my $max    = shift(@values);
		my $dp     = shift(@values);  #print GLOBAL_LOG "  * $name \n";	
		if ($line =~m/CONSTANT/) {$self->{STEP_CONSTANT_2}->{$code} = pop(@values); $self->{STEP_COMMENT_2}->{$code} = pop(@values);}
	    if (defined $self->{STEP_COMMENT_2}->{$code}) {my $const = $self->{STEP_CONSTANT_2}->{$code};  $mean = $mean + $const;}
		
		for (1 .. 99)
		  {
		    unless (defined $values[3]) {last;}
		    my $step = shift(@values);   # 1-8
			my $pos  = shift(@values);   # 1-5
			$self->{STEP_2}->{$code}->{$step}->{$pos}->{min}  = shift(@values);
			$self->{STEP_2}->{$code}->{$step}->{$pos}->{mean} = shift(@values);
			$self->{STEP_2}->{$code}->{$step}->{$pos}->{max}  = shift(@values);
			
		    if (defined $self->{STEP_COMMENT_2}->{$code}) 
		      {
			    my $const = $self->{STEP_CONSTANT_2}->{$code};
			    if ($pos == 1) 
				  {
				    $self->{STEP_2}->{$code}->{$step}->{$pos}->{mean} = $self->{STEP_2}->{$code}->{$step}->{$pos}->{mean} + $const; 
				    $self->{STEP_2}->{$code}->{$step}->{$pos}->{max}  = $self->{STEP_2}->{$code}->{$step}->{$pos}->{max}  + $const; 
					$self->{STEP_2}->{$code}->{$step}->{$pos}->{min}  = $self->{STEP_2}->{$code}->{$step}->{$pos}->{min}  + $const;
				  }
			    else {$self->{STEP_2}->{$code}->{$step}->{$pos}->{mean} = $self->{STEP_2}->{$code}->{$step}->{$pos}->{mean} + $const;}
			  }				
		  }

		my $mn    = unpack('A1 A4', $code);
		next unless ($mn =~m/$mission/);
		
	    if((defined $min) && (defined $max))
	      {
	        $code  =~s/\s*//;
	        $self->{DATA_2}->{$code}->{name} = $name;
		    $self->{DATA_2}->{$code}->{max}  = $max;
		    $self->{DATA_2}->{$code}->{min}  = $min;
		    $self->{DATA_2}->{$code}->{mean} = $mean;
		    $self->{DATA_2}->{$code}->{dp}   = $dp;
	        if ($max  >  $max_y)
		     {
		       $max_y  = $max;          #Y-axis
			   $self->{neutral_axis_y_shift} = 290;
		     }
			if (abs($min) > $max_y) {$max_y = abs($min); $self->{neutral_axis_y_shift} = 170;}
			 
		    #$num++;   
	        #if ($num >  $max_x)
		    #{
		    #  $max_x  = $num;    #X-axis
		    #}
		    push(@{$self->{ALL_CODES_2}}, $code);   
	      }
	  }

    ##### Multipl. factors for graph
    my $scale_y = 1;
    my $scale_x = 1.5 * 60 / $max_x;
    if   (($max_y   >  800) && ($max_y   < 1800))      {$scale_y = 0.125;} # Stress / Load Value Y-dir	
    if   (($max_y   >  500) && ($max_y   <  800))      {$scale_y = 0.25;}  # Stress / Load Value Y-dir
    elsif(($max_y   >  350) && ($max_y   <  500))      {$scale_y = 0.5;}   # Stress / Load Value Y-dir	
    elsif(($max_y   >  125) && ($max_y   <  350))      {$scale_y = 1.0;}
    elsif(($max_y   > 62.5) && ($max_y   <  125))      {$scale_y = 2.0;}
    elsif(($max_y   > 31.25) && ($max_y   <  62.5))    {$scale_y = 4.0;}
    elsif(($max_y   > 15.625) && ($max_y   <  31.25))  {$scale_y = 8.0;}
    elsif(($max_y   > 7.8125) && ($max_y   <  15.625)) {$scale_y = 16.0;} 
    elsif(($max_y   > 3.91) && ($max_y   <  7.8125))   {$scale_y = 32.0;} 
    elsif(($max_y   > 1.95) && ($max_y   <  3.91))     {$scale_y = 64.0;}  
    else {$scale_y   = 128.0;}
	
    #$scale_y = 300 / $max_y;	
	if ($self->{do_fixed_scale_y} =~m/yes/i) {$scale_y = $self->{force_scale_y_to};} 
	
    #Shift Y direction
    my $shy = $self->{neutral_axis_y_shift} ; # 290;
    print GLOBAL_LOG "  * Scale Factors: X|$scale_x|  Y|$scale_y| SeG|$self->{total_segments_for_plotting}|\n";	
	if    ($file =~m/\\|\//)   {$file   =~m/^.+[\\|\/]\s*(.+)$/;  $file   = $1;}
	if    ($file_2 =~m/\\|\//) {$file_2 =~m/^.+[\\|\/]\s*(.+)$/;  $file_2 = $1;}
	  
    ##############################################
    ##### DRAW PLOT
    my $tc         = 'brown';
    my $at         = 'black';
	my $plot_font  = 'Courier 8';
    my $titlefont  = 'Courier 10 italic';
	my $mini_font  = 'Courier 6 bold';
	my $thick_font = 'Courier 8 bold';	

    ##### AXIS created here
    $canvas->createLine(50, $shy, 950, $shy, -width => 2);  # X
    $canvas->createLine(50, 450,  50,  50,   -width => 2);  # Y
    $canvas->createText(
			500, 25, 
			-text => "Extreme 1/Block Profile [MaxSeverityLevel: 8 MaxIncrePos: $self->{upto_incre_level} $self->{plot_pos_neg_side}]", 
			-font => $titlefont,
			-fill => $tc,
		       );

    ##### Vertical GRIDS created here
    for (1 .. 20)
      {
	    my $x = (50 + ($_ * 30) * 1.5);
	    $canvas->createLine($x, 450, $x, 50, -width => 1, -fill => 'yellow');
      }
    ##### Horizontal GRIDS created here
    for (0 .. 10)
      {
	my $y =  450 - ($_ * 40);
	$canvas->createLine(50, $y, 950, $y, -width => 1, -fill => 'brown');
      }
    ##### MARKS on X-AXIS & Y-AXIS  created here
    my($i, $x, $y, $point, $item);

    for ($i = -4; $i <= 6; $i++) 
      {
	$y =  $shy - ($i * 40)* $scale_y;
	$canvas->createLine(50, $y, 55, $y, -width => 2);
	$canvas->createText(
			    46, $y,
			    -text   => $i * 50.0, 
			    -anchor => 'e',
			    -font   => $plot_font
			   );
      } # forend

	# List the Files on Canvas
	$canvas->createLine(160, 40, 190,40, -fill => $colour_D1, -width => 3);
    $canvas->createText(200, 40, -text => "[Mission: $mission] $file",   -font => $thick_font, -fill => $colour_D1, -anchor  => 'w',);
	$canvas->createLine(570, 40, 600,40, -fill => $colour_D2, -width => 3);
    $canvas->createText(610, 40, -text => "[Mission: $mission] $file_2", -font => $thick_font, -fill => $colour_D2, -anchor  => 'w',);
    # WRITE LEGENDS
    my $x_legend = $canvas->createText(
				       120, 80, # X, Y
				       -text    => "EPuRE", 
				       -anchor  => 'e',
				       -font    => 'Courier 20 bold',
				       -fill    => 'pink',
				       -stipple => 'gray50',
				       -tags    => 'item',					   
				      ); 
    my $y_legend = $canvas->createText(
				       80, 40, # X, Y
				       -text => "Load Unit [Mpa/KN]", 
				       -font => $thick_font,
				       -fill => $at,
				      ); 
    my $j    = 0;
	my $k    = 0;
	my $sel  = 0;
    my $x_p  = 'X'; my $m_p = 'M'; my $dp_p = 'DP';
	
	my $plotwidth = 0.5;  
	if    ($self->{total_segments_for_plotting} < 25) {$plotwidth = 1.0;}
	elsif ($self->{total_segments_for_plotting} < 40) {$plotwidth = 0.8;}
	elsif ($self->{total_segments_for_plotting} < 80) {$plotwidth = 0.5;}	
	else  {$plotwidth = 0.3;}
	
    foreach (@{$self->{ALL_CODES}})
      {
	my $code = $_;
	my $name = $self->{DATA}->{$code}->{name};
	my $max  = $self->{DATA}->{$code}->{max};
	my $min  = $self->{DATA}->{$code}->{min};
	my $mean = $self->{DATA}->{$code}->{mean};
	my $dpv  = $self->{DATA}->{$code}->{dp};
	$j++; $k++;
	
	my $x   =  50 + ($j * 10)   * $scale_x;
	my $m   = $shy - (4 * $mean)* $scale_y / 5;
	my $y_1 = $shy - (4 * $max) * $scale_y / 5;
	my $y_2 = $shy - (4 * $min) * $scale_y / 5;
	my $dp  = $shy - (4 * $dpv) * $scale_y / 5;
	
	if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m; $dp_p = $dp;}
	if ($k > 5) {$k = 1;}
	#my $scy = 410 + 10*$k;    # include zig-zag labels for longer CODES!
	my $scy = 410 + 10;     # No zig-zag labels

    my $g    = 0; 
	if ($self->{plot_only_1g_segment} =~m/YES/i) 
	  {
	    foreach (@{$self->{plot_this_1g}})  {if ($_ =~m/$code/) {$g = 1;};}; next unless ($g > 0); $plotwidth = 4;
		$x   =  60 + ($sel * 200)   * $scale_x;		$sel++;
	  } 
	
	# write code 
	my ($z,$seg) = unpack('A2A2',$code); $seg = '['. $seg . ']';
	$canvas->createText($x, $scy,-text => $seg, -fill => $colour_D1, -anchor => 'n',-font   => $mini_font) unless ($self->{write_codes_info} < 1);

	for($i = 0; $i <= 20; $i++) {$canvas->createLine($x, 405, $x, 415, -width => 2);}
    $canvas->createLine($x_p, $dp_p,$x, $dp,  -fill => $colour_dp,    -width => 1);	  # Do not draw DP Line	
    #$canvas->createLine($x_p, $m_p, $x,  $m,  -fill => $colour_mean,  -width => 1);
    $canvas->createLine($x_p, $m_p, $x,  $m_p,-fill => $colour_mean,  -width => 1);
	my $nameOval = $name .'_['. $code . ']_steady';
	$canvas->createLine($x,   $m,   $x,$y_1,  -fill => $colour_upper[0], -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|UP/i);
	$canvas->createLine($x,   $m,   $x,$y_2,  -fill => $colour_lower[0], -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|DOWN/i);
    $self->{CANVAS}->bind("$nameOval", '<Any-Enter>' => [sub{$self->{INFO} = "$nameOval"; shift->itemconfigure(@_);}, 'current',]);		
	# write name e.g EXC_N=1_TXOT_F1
	$name =~m/EXC\_N.+\_(.+)\_F.+/; my $na = $1; $na =~s/^-//;	
	$canvas->createText($x, $y_1-10,-text => $na, -anchor => 'w',-font   => $mini_font) if ($self->{show_text_more} > 0);
	#$canvas->createText($x, $scy,-text => $na, -anchor => 'n',-font   => $plot_font);	
	
	$x_p  = $x;
	$m_p  = $m;
	$dp_p = $dp;
	my $mean_point = $canvas->createOval(
					     $x-1, $m-1,
					     $x+1, $m+1,
					     -width => 1,
					     -outline => $colour_mean,
					    #-fill    => $colour_mean,
					    );
	my $dp_point   = $canvas->createOval(
					     $x-1, $dp_p-1,
					     $x+1, $dp_p+1,
					     -width => 1,
					     -outline => $colour_dp,
					    #-fill    => $colour_dp,
					    );

	# Additional Steps 1 to 99 Possibilities	

	for (1 .. 9)
	  { 
	    my $step = $_;  
		if ($self->{plot_only_1g_segment} =~m/YES/i) {$x = $x + 20 * ($scale_x / 2.0);}
		else {$x = $x + 2 * ($scale_x / 2.0);}  
	    my $up   = $m; 
	    my $lw   = $m;            # Step 1-8
		foreach(sort keys %{$self->{STEP}->{$code}->{$step}})  # Incre Pos. 1-5
		  {
		   my $pos  = $_;    # incre Pos 1-5
           if ($pos > $self->{upto_incre_level}) {next;}
		   my $m_i  = $shy - (4 * $self->{STEP}->{$code}->{$step}->{$pos}->{mean}) * $scale_y / 5;
	       my $smax = $shy - (4 * $self->{STEP}->{$code}->{$step}->{$pos}->{max})  * $scale_y / 5;
	       my $smin = $shy - (4 * $self->{STEP}->{$code}->{$step}->{$pos}->{min})  * $scale_y / 5;
		   if ($pos < 2) {$up = $m_i; $lw = $m_i;}
		   if ($self->{plot_only_1g_segment} =~m/YES/i) {$x = $x + 4;}
		   
		   $nameOval = $name .'_['. $code . ']_ip' . $pos .'_step'. $step;
           $canvas->createLine($x,$up,$x,$smax, -fill => $colour_D1, -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|UP/i);
           $canvas->createLine($x,$lw,$x,$smin, -fill => $colour_D1, -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|DOWN/i);
           #  PLOT #1
           $up = $smax; 
           $lw = $smin; 
		   if ($pos > 2) {$up = $m_i; $lw = $m_i;} # This line forces the 3rd incre_pos onwards to start from Mean (i.e. not from Max as default)
           $self->{CANVAS}->bind("$nameOval", '<Any-Enter>' => [sub{$self->{INFO} = "$nameOval"; shift->itemconfigure(@_);}, 'current',]);			   
		   if ($pos > 2) {$up = $m_i; $lw = $m_i;} # This line forces the 3rd incre_pos onwards to start from Mean (i.e. not from Max as default)
		  }
	  }
      }

    # No. 2 Plot! ################################################################
    $j   = 0;   $k   = 0;   $sel  = 0;
    $x_p = 'X'; $m_p = 'M'; $dp_p = 'DP';
	
    foreach (@{$self->{ALL_CODES_2}})
      {
	my $code = $_;
	my $name = $self->{DATA_2}->{$code}->{name};
	my $max  = $self->{DATA_2}->{$code}->{max};
	my $min  = $self->{DATA_2}->{$code}->{min};
	my $mean = $self->{DATA_2}->{$code}->{mean};
	my $dpv  = $self->{DATA_2}->{$code}->{dp};
	$j++; $k++;
	
	my $x   =  50 + ($j * 10)   * $scale_x;
	my $m   = $shy - (4 * $mean)* $scale_y / 5;
	my $y_1 = $shy - (4 * $max) * $scale_y / 5;
	my $y_2 = $shy - (4 * $min) * $scale_y / 5;
	my $dp  = $shy - (4 * $dpv) * $scale_y / 5;
	
	if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m; $dp_p = $dp;}
	if ($k > 5) {$k = 1;}
	my $scy = 420 + 10*$k;    # include zig-zag labels for longer CODES!

    my $g    = 0; 
	if ($self->{plot_only_1g_segment} =~m/YES/i) 
	  {
	    foreach (@{$self->{plot_this_1g}})  {if ($_ =~m/$code/) {$g = 1;};}; next unless ($g > 0); $plotwidth = 4;
		$x   =  60 + ($sel * 200)   * $scale_x;		$sel++;
	  }
	
	# write code 
	my ($z,$seg) = unpack('A2A2',$code); $seg = '['. $seg . ']';
	$canvas->createText($x, $scy,-text => $seg, -fill => $colour_D2, -anchor => 'n',-font   => $mini_font) unless ($self->{write_codes_info} < 1);

	for($i = 0; $i <= 20; $i++) {$canvas->createLine($x, 405, $x, 415,   -width => 2);}
    #$canvas->createLine($x_p, $dp_p,$x, $dp,  -fill => $colour_dp_2,    -width => 1);	  # Do not draw DP Line	
    $canvas->createLine($x_p, $m_p, $x,  $m_p,-fill => $colour_mean_2,   -width => 1);
	my $nameOval = $name .'_['. $code . ']_steady';
	$canvas->createLine($x,   $m,   $x,$y_1,  -fill => $colour_upper[0], -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|UP/i);
	$canvas->createLine($x,   $m,   $x,$y_2,  -fill => $colour_lower[0], -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|DOWN/i);
    $self->{CANVAS}->bind("$nameOval", '<Any-Enter>' => [sub{$self->{INFO} = "$nameOval"; shift->itemconfigure(@_);}, 'current',]);	
	
	$x_p  = $x;
	$m_p  = $m;
	$dp_p = $dp;
	my $mean_point = $canvas->createOval(
					     $x-1, $m-1,
					     $x+1, $m+1,
					     -width => 1,
					     -outline => $colour_mean,
					    #-fill    => $colour_mean,
					    );
	my $dp_point   = $canvas->createOval(
					     $x-1, $dp_p-1,
					     $x+1, $dp_p+1,
					     -width => 4,
					     -outline => $colour_dp_2,
					    #-fill    => $colour_dp_2,
					    );

	# Additional Steps 1 to 99 Possibilities	

	for (1 .. 9)
	  { 
	    my $step = $_;  
		if ($self->{plot_only_1g_segment} =~m/YES/i) {$x = $x + 20 * ($scale_x / 2.0);}
		else {$x = $x + 2 * ($scale_x / 2.0);}  
	    my $up   = $m; 
	    my $lw   = $m;            # Step 1-8
		foreach(sort keys %{$self->{STEP_2}->{$code}->{$step}})  # Incre Pos. 1-5
		  {
		   my $pos  = $_;    # incre Pos 1-5
           if ($pos > $self->{upto_incre_level}) {next;}
		   my $m_i  = $shy - (4 * $self->{STEP_2}->{$code}->{$step}->{$pos}->{mean}) * $scale_y / 5;
	       my $smax = $shy - (4 * $self->{STEP_2}->{$code}->{$step}->{$pos}->{max})  * $scale_y / 5;
	       my $smin = $shy - (4 * $self->{STEP_2}->{$code}->{$step}->{$pos}->{min})  * $scale_y / 5;
		   if ($pos < 2) {$up = $m_i; $lw = $m_i;}
		   if ($self->{plot_only_1g_segment} =~m/YES/i) {$x = $x + 4;}
		   $nameOval = $name .'_['. $code . ']_ip' . $pos .'_step'. $step;
           $canvas->createLine($x,$up,$x,$smax, -fill => $colour_D2, -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|UP/i);
           $canvas->createLine($x,$lw,$x,$smin, -fill => $colour_D2, -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|DOWN/i);
           $up = $smax; 
           $lw = $smin; 
		   if ($pos > 2) {$up = $m_i; $lw = $m_i;} # This line forces the 3rd incre_pos onwards to start from Mean (i.e. not from Max as default)
           $self->{CANVAS}->bind("$nameOval", '<Any-Enter>' => [sub{$self->{INFO} = "$nameOval"; shift->itemconfigure(@_);}, 'current',]);		   
		  }
	  }
      }	  # No. 2 Plot!
  } # end data organizer

 
	 
sub plot_organized_data_on_Tk_canvas_single
  {
    my $self         = shift;
    my $file         = $self->{PROFILE_FILE};
	my $canvas       = $self->{CANVAS};
    my @colour_upper = qw/blue blue green Coral DarkViolet Goldenrod brown Orchid/;      #my @colour_upper = qw/Blue Blue LawnGreen DarkOrchid1 Orange DeepPink/;
    my @colour_lower = qw/red red gold cyan pink purple LightCyan Magenta/;	             #my @colour_lower = qw/Red Red OliveDrab1 Gold Magenta Orchid DarkViolet/;
    my $colour_mean  = 'black';
    my $colour_dp    = 'purple';

    my @material_colours = qw/yellow red blue green black cyan purple orange pink gold grey brown violet white/;	
    my @plotdata         = @{$self->{all_profile_file_contents}}; #print GLOBAL_LOG "@plotdata\n";
    ##### Find maximum DATA values
    my $max_y   = 0.0;
    my $max_x   = 0;
    my $num     = 0;
    my $mission = $self->{mission};
    @{$self->{ALL_CODES}} = ();
	
    foreach (@plotdata)
      {
	    if (($_  =~m/^\s*$/) || ($_ =~m/\#/))    { next;}
	    my $line = $_; $line =~s/^\s*//;
		my @values = split('\s+', $line);
		my $name   = shift(@values);
		my $code   = shift(@values);
		my $min    = shift(@values);
		my $mean   = shift(@values);
		my $max    = shift(@values);
		my $dp     = shift(@values);  #print GLOBAL_LOG "  * $name \n";
		if ($line =~m/CONSTANT/) {$self->{STEP_CONSTANT}->{$code} = pop(@values); $self->{STEP_COMMENT}->{$code} = pop(@values);}
	    if (defined $self->{STEP_COMMENT}->{$code}) {my $const = $self->{STEP_CONSTANT}->{$code};  $mean = $mean + $const;}
		
		for (1 .. 99)
		  {
		    unless (defined $values[3]) {last;}
		    my $step = shift(@values);   # 1-8
			my $pos  = shift(@values);   # 1-5
			$self->{STEP}->{$code}->{$step}->{$pos}->{min}  = shift(@values);
			$self->{STEP}->{$code}->{$step}->{$pos}->{mean} = shift(@values);
			$self->{STEP}->{$code}->{$step}->{$pos}->{max}  = shift(@values);

		    if (defined $self->{STEP_COMMENT}->{$code}) 
		      {
			    my $const = $self->{STEP_CONSTANT}->{$code};
			    if ($pos == 1) 
				  {
				    $self->{STEP}->{$code}->{$step}->{$pos}->{mean} = $self->{STEP}->{$code}->{$step}->{$pos}->{mean} + $const; 
				    $self->{STEP}->{$code}->{$step}->{$pos}->{max}  = $self->{STEP}->{$code}->{$step}->{$pos}->{max}  + $const; 
					$self->{STEP}->{$code}->{$step}->{$pos}->{min}  = $self->{STEP}->{$code}->{$step}->{$pos}->{min}  + $const;
				  }
			    else {$self->{STEP}->{$code}->{$step}->{$pos}->{mean} = $self->{STEP}->{$code}->{$step}->{$pos}->{mean} + $const;}
			  }	
		  }

		my $mn    = unpack('A1 A4', $code);
		next unless ($mn =~m/$mission/);
		
	    if((defined $min) && (defined $max))
	      {
	        $code  =~s/\s*//;
	        $self->{DATA}->{$code}->{name} = $name;
		    $self->{DATA}->{$code}->{max}  = $max;
		    $self->{DATA}->{$code}->{min}  = $min;
		    $self->{DATA}->{$code}->{mean} = $mean;
		    $self->{DATA}->{$code}->{dp}   = $dp;
	        if ($max  >  $max_y)
		     {
		       $max_y  = $max;          #Y-axis
			   $self->{neutral_axis_y_shift} = 290;
		     }
			if (abs($min) > $max_y) {$max_y = abs($min); $self->{neutral_axis_y_shift} = 170;}
			 
		    $num++;   
	        if ($num >  $max_x)  {$max_x  = $num;}    #X-axis
		    push(@{$self->{ALL_CODES}}, $code);   
	      }
	  }
    $self->{total_segments_for_plotting} = $num;

    ##### Multipl. factors for graph
    my $scale_y = 1;
    my $scale_x = 1.5 * 60 / $max_x;
    if   (($max_y   >  800) && ($max_y   < 1800))      {$scale_y = 0.125;} # Stress / Load Value Y-dir	
    if   (($max_y   >  500) && ($max_y   <  800))      {$scale_y = 0.25;} # Stress / Load Value Y-dir
    elsif(($max_y   >  350) && ($max_y   <  500))      {$scale_y = 0.5;}  # Stress / Load Value Y-dir	
    elsif(($max_y   >  125) && ($max_y   <  350))      {$scale_y = 1.0;}
    elsif(($max_y   > 62.5) && ($max_y   <  125))      {$scale_y = 2.0;}
    elsif(($max_y   > 31.25) && ($max_y   <  62.5))    {$scale_y = 4.0;}
    elsif(($max_y   > 15.625) && ($max_y   <  31.25))  {$scale_y = 8.0;}
    elsif(($max_y   > 7.8125) && ($max_y   <  15.625)) {$scale_y = 16.0;} 
    elsif(($max_y   > 3.91) && ($max_y   <  7.8125))   {$scale_y = 32.0;} 
    elsif(($max_y   > 1.95) && ($max_y   <  3.91))     {$scale_y = 64.0;}  
    else {$scale_y   = 128.0;}
	
    #$scale_y = 300 / $max_y;	
	if ($self->{do_fixed_scale_y} =~m/yes/i) {$scale_y = $self->{force_scale_y_to};} 
	
    #Shift Y direction
    my $shy = $self->{neutral_axis_y_shift} ; # 290;
    print GLOBAL_LOG "  * Scale Factors: X|$scale_x|  Y|$scale_y| SeG|$self->{total_segments_for_plotting}|\n";	
	if    ($file =~m/\\|\//) {$file =~m/^.+[\\|\/]\s*(.+)$/;  $file = $1;}
	  
    ##############################################
    ##### DRAW PLOT
    my $tc         = 'brown';
    my $at         = 'black';
	my $plot_font  = 'Courier 8';
    my $titlefont  = 'Courier 10 italic';
	my $mini_font  = 'Courier 6 bold';
	my $thick_font = 'Courier 8 bold';	

    ##### AXIS created here
    $canvas->createLine(50, $shy, 950, $shy, -width => 2);  # X
    $canvas->createLine(50, 450,  50,  50,   -width => 2);  # Y
    $canvas->createText(
			500, 25, 
			-text => "Extreme 1/Block Profile [Mission: $mission] [MaxSeverityLevel: 8 MaxIncrePos: $self->{upto_incre_level} $self->{plot_pos_neg_side}]  //File: $file//", 
			-font => $titlefont,
			-fill => $tc,
		       );
    ##### Vertical GRIDS created here
    for (1 .. 20)
      {
	    my $x = (50 + ($_ * 30) * 1.5);
	    $canvas->createLine($x, 450, $x, 50, -width => 1, -fill => 'yellow');
      }
    ##### Horizontal GRIDS created here
    for (0 .. 10)
      {
	my $y =  450 - ($_ * 40);
	$canvas->createLine(50, $y, 950, $y, -width => 1, -fill => 'brown');
      }
    ##### MARKS on X-AXIS & Y-AXIS  created here
    my($i, $x, $y, $point, $item);

    for ($i = -4; $i <= 6; $i++) 
      {
	$y =  $shy - ($i * 40)* $scale_y;
	$canvas->createLine(50, $y, 55, $y, -width => 2);
	$canvas->createText(
			    46, $y,
			    -text   => $i * 50.0, 
			    -anchor => 'e',
			    -font   => $plot_font
			   );
      } # forend

	# Draw Upper Colours Legend
	foreach (1 .. 5) # Upper Side
	  {
	    my $cl   = $colour_upper[$_];
		my $clxA = 150 + $_ * 50;  my $clxB = $clxA + 30;  my $clxT = $clxB + 10;
	    $canvas->createLine($clxA,40, $clxB,40, -fill => $cl, -width => 3);
		$canvas->createText($clxT, 40, -text => sprintf("+%s","$_"), -font => $thick_font, -fill => $cl,);
        if ($_ == 5) {$clxT = $clxT + 50; $canvas->createText($clxT, 40, -text => sprintf("[%8s]","Incr+Pos"), -font => $thick_font, -fill => $tc,);} 		
	  }
	foreach (1 .. 5) # Lower Side
	  {
	    my $cl   = $colour_lower[$_];
		my $clxA = 500 + $_ * 50;  my $clxB = $clxA + 30;  my $clxT = $clxB + 10;
	    $canvas->createLine($clxA,40, $clxB,40, -fill => $cl, -width => 3);
		$canvas->createText($clxT, 40, -text => sprintf("-%s","$_"), -font => $thick_font, -fill => $cl,);
        if ($_ == 5) {$clxT = $clxT + 50; $canvas->createText($clxT, 40, -text => sprintf("[%8s]","Incr-Pos"), -font => $thick_font, -fill => $tc,);} 		
	  }	   
    # WRITE LEGENDS
    my $x_legend = $canvas->createText(
				       120, 80, # X, Y
				       -text    => "EPuRE", 
				       -anchor  => 'e',
				       -font    => 'Courier 20 bold',
				       -fill    => 'pink',
				       -stipple => 'gray50',
				       -tags    => 'item',					   
				      ); 
    my $y_legend = $canvas->createText(
				       80, 40, # X, Y
				       -text => "Load Unit [Mpa/KN]", 
				       -font => $thick_font,
				       -fill => $at,
				      ); 
    my $j    = 0;
	my $k    = 0;
	my $sel  = 0;
    my $x_p  = 'X'; my $m_p = 'M'; my $dp_p = 'DP';
	
	my $plotwidth = 0.5;  
	if    ($self->{total_segments_for_plotting} < 25) {$plotwidth = 1.0;}
	elsif ($self->{total_segments_for_plotting} < 40) {$plotwidth = 0.8;}
	elsif ($self->{total_segments_for_plotting} < 80) {$plotwidth = 0.5;}	
	else  {$plotwidth = 0.3;}
	
    foreach (@{$self->{ALL_CODES}})
      {
	my $code = $_;
	my $name = $self->{DATA}->{$code}->{name};
	my $max  = $self->{DATA}->{$code}->{max};
	my $min  = $self->{DATA}->{$code}->{min};
	my $mean = $self->{DATA}->{$code}->{mean};
	my $dpv  = $self->{DATA}->{$code}->{dp};
	$j++; $k++;
	
	my $x   =  50 + ($j * 10)   * $scale_x;
	my $m   = $shy - (4 * $mean)* $scale_y / 5;
	my $y_1 = $shy - (4 * $max) * $scale_y / 5;
	my $y_2 = $shy - (4 * $min) * $scale_y / 5;
	my $dp  = $shy - (4 * $dpv) * $scale_y / 5;
    
	if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m; $dp_p = $dp;}
	if ($k > 5) {$k = 1;}
	#my $scy = 410 + 10*$k;    # include zig-zag labels for longer CODES!
	my $scy = 410 + 10;     # No zig-zag labels

    my $g    = 0; 
	if ($self->{plot_only_1g_segment} =~m/YES/i) 
	  {
	    foreach (@{$self->{plot_this_1g}})  {if ($_ =~m/$code/) {$g = 1;};}; next unless ($g > 0); $plotwidth = 4;
		$x   =  60 + ($sel * 200)   * $scale_x;		$sel++;
	  } 
	
	# write code 
	my ($z,$seg) = unpack('A2A2',$code); $seg = '['. $seg . ']';
	$canvas->createText($x, $scy,-text => $seg, -anchor => 'n',-font   => $mini_font) unless ($self->{write_codes_info} < 1);

	for($i = 0; $i <= 20; $i++) {$canvas->createLine($x, 405, $x, 415, -width => 2);}
    $canvas->createLine($x_p, $dp_p,$x, $dp,  -fill => $colour_dp,    -width => 1);	  # Do not draw DP Line	
    #$canvas->createLine($x_p, $m_p, $x,  $m,  -fill => $colour_mean,  -width => 1);
    $canvas->createLine($x_p, $m_p, $x,  $m_p,-fill => $colour_mean,  -width => 1);
	my $nameOval = $name .'_['. $code . ']_steady';
	$canvas->createLine($x,   $m,   $x,$y_1,  -fill => $colour_upper[0], -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|UP/i);
	$canvas->createLine($x,   $m,   $x,$y_2,  -fill => $colour_lower[0], -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|DOWN/i);
    $self->{CANVAS}->bind("$nameOval", '<Any-Enter>' => [sub{$self->{INFO} = "$nameOval"; shift->itemconfigure(@_);}, 'current',]);	
	# write name e.g EXC_N=1_TXOT_F1
	$name =~m/EXC\_N.+\_(.+)\_F.+/; my $na = $1; $na =~s/^-//;	
	$canvas->createText($x, $y_1-10,-text => $na, -anchor => 'w',-font   => $mini_font) if ($self->{show_text_more} > 0);
	#$canvas->createText($x, $scy,-text => $na, -anchor => 'n',-font   => $plot_font);	
	
	$x_p  = $x;
	$m_p  = $m;
	$dp_p = $dp;
	my $mean_point = $canvas->createOval(
					     $x-1, $m-1,
					     $x+1, $m+1,
					     -width => 1,
					     -outline => $colour_mean,
					    #-fill    => $colour_mean,
					    );
	my $dp_point   = $canvas->createOval(
					     $x-1, $dp_p-1,
					     $x+1, $dp_p+1,
					     -width => 1,
					     -outline => $colour_dp,
					    #-fill    => $colour_dp,
					    );

	# Additional Steps 1 to 99 Possibilities	

	for (1 .. 9)
	  { 
	    my $step = $_;  
		if ($self->{plot_only_1g_segment} =~m/YES/i) {$x = $x + 20 * ($scale_x / 2.0);}
		else {$x = $x + 2 * ($scale_x / 2.0);}  
	    my $up   = $m; 
	    my $lw   = $m;            # Step 1-8
		foreach(sort keys %{$self->{STEP}->{$code}->{$step}})  # Incre Pos. 1-5
		  {
		   my $pos  = $_;    # incre Pos 1-5
           if ($pos > $self->{upto_incre_level}) {next;}
		   my $m_i  = $shy - (4 * $self->{STEP}->{$code}->{$step}->{$pos}->{mean}) * $scale_y / 5;
	       my $smax = $shy - (4 * $self->{STEP}->{$code}->{$step}->{$pos}->{max})  * $scale_y / 5;
	       my $smin = $shy - (4 * $self->{STEP}->{$code}->{$step}->{$pos}->{min})  * $scale_y / 5;
		   if ($pos < 2) {$up = $m_i; $lw = $m_i;}
		   if ($self->{plot_only_1g_segment} =~m/YES/i) {$x = $x + 4;}
		   $nameOval = $name .'_['. $code . ']_ip' . $pos .'_step'. $step;
           $canvas->createLine($x,$up,$x,$smax, -fill => $colour_upper[$pos], -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|UP/i);
           $canvas->createLine($x,$lw,$x,$smin, -fill => $colour_lower[$pos], -width => $plotwidth, -tag => ['oval', "$nameOval"]) if ($self->{plot_pos_neg_side} =~m/ALL|DOWN/i);
           $up = $smax; 
           $lw = $smin; 
		   if ($pos > 2) {$up = $m_i; $lw = $m_i;} # This line forces the 3rd incre_pos onwards to start from Mean (i.e. not from Max as default)
           $self->{CANVAS}->bind("$nameOval", '<Any-Enter>' => [sub{$self->{INFO} = "$nameOval"; shift->itemconfigure(@_);}, 'current',]);	
		  }
	  }
      }
  } # end data organizer

   
   
sub build_Tk_mainframe_objects
   {
	my $self = shift;
    my $mw     = MainWindow->new();
    $mw->title("Plot Min-Mean.Max Flight!  version  4.000");
    $mw->optionAdd('*font' => 'Courier 10');
    $mw->geometry("1000x600+50+50");		
    $self->{MW} =  $mw;
    $self->{initializatorator}    = 0;	
    my $frame_A  = $mw->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'x',);
    my $frame_B  = $mw->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'both',);
	my $frame_C  = $mw->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'x',);
	$self->{frame_B}  = $frame_B;

    my $graph_Menu = $self->{MENU} = $frame_A->Menubutton(-text   => 'ViewMenu', -underline   => 0,-tearoff     => 1, )->pack(-side => 'left',);
    my $zoom_in    = $graph_Menu->radiobutton( -label    => 'Zoom ++',-value    => 'ZOOM',-command   => sub  {  my ($l, $r, $t, $b) = $self->{CANVAS}->bbox('all');  $self->{CANVAS}->scale("all", "$l","$r", 2.0, 2.0); },);
    my $zoomout    = $graph_Menu->radiobutton(-label     => 'Zoom --', -value   => 'OUT', -command   => sub    {  my ($l, $r, $t, $b) = $self->{CANVAS}->bbox('all');$self->{CANVAS}->scale("all", "$l","$r", 0.5, 0.5); },);
    my $get_image  = $graph_Menu->radiobutton(-label  => 'Cen Image',-value   => 'GET',-command   => sub{ my ($l, $r, $t, $b) = $self->{CANVAS}->bbox('all'); $self->{CANVAS}->configure(-scrollregion => ["$l", "$r", "$t", "$b"]);},);
					     
    my $fit_screen = $graph_Menu->radiobutton(-label     => 'Fit Image',-value    => 'FIT',-command  => sub
					      { 
						my ($l, $r, $t, $b) = $self->{CANVAS}->bbox('all');
						my $cur_width  = $self->{CANVAS}->width();
						my $cur_height = $self->{CANVAS}->height();
						
						my $image_w = $t - $l;
						my $image_h = $b - $r;
						
						my $x_ratio = $cur_width/$image_w;
						my $y_ratio = $cur_height/$image_h;
						
						$self->{CANVAS}->scale("all", "$l","$r", "$x_ratio","$y_ratio");
						#$canvas->configure(-scrollregion => ["$l", "$r", "$t", "$b"]);
					      },);
			
    my $postscr = $graph_Menu->radiobutton(
					   -label     => 'Gen PostScript',
					   -value     => 'PS',
					   -command   => sub 
					   {
					     my $mission = $self->{mission};
					     my $file_1 = $self->{PROFILE_FILE} . "_M" . $mission . '_P' . $self->{upto_incre_level} . '_' . $self->{plot_pos_neg_side} . '.ps';					   
					     if ($self->{plot_only_1g_segment} =~m/YES/i) {$file_1 = $self->{PROFILE_FILE} . "_M" . $mission . '_G' . @{$self->{plot_this_1g}}[0] . '_P' . $self->{upto_incre_level} . '_' . $self->{plot_pos_neg_side} . '.ps';}
					     $self->{CANVAS}->postscript(
								 -file      => "$file_1",
								 -colormode => "color",
								 -rotate    => 1,
								 -width  => '1050',
								 -height => '600',
								 -pagewidth  => '1050',
								 -pageheight => '600',
								 #-pagex      => '100',
								 #-pagey      => '100',
								 -pageanchor => 'center',
								);
					
					   },);
    my $txt_item = $graph_Menu->radiobutton(-label => 'EDiToR!',-underline   => 0,-command  => sub {&activate_text_editor($self);});	
    $self->{show_text_more} = 1;
	
	# COLORS ###########################################################
    @{$self->{colors_available}}  = qw/gold red green black cyan purple orange pink yellow grey brown violet white blue/;
    $self->{color_menu} =  $frame_A->Optionmenu(
			 -options      => [@{$self->{colors_available}}],
			 -textvariable => \$self->{color_for_2_now},
			 -variable     => \$self->{color_for_2_now},
			 -background   => $self->{color_for_2_now},
			 -justify      => 'right',-relief  => 'groove',-width => 8,
			 -command      => sub
			                   {
			                       return unless ($self->{initializatorator} > 0);
								   $self->{color_menu}->configure(-background   => $self->{color_for_2_now});
			                       &sort_out_the_data_with_reference_to_selected_options($self);
			                   },)->pack(-side   => 'right', );	
	####################################################################
	
    $frame_A->Button( -text  => "...AddData", -relief  => 'groove',-command => sub {$self->{OPERATION} = 'open'; 
	                 &fileDialog_2($self);  unless(defined $self->{PROFILE_FILE_2}) {$self->{added_file} = 1; return;}; $self->{steady_code_selected} = 'ALL';
					 $self->{added_file} = 2;					   # Fixed to only do 2 files max!!!!!!!!!!!!
	                 &sort_out_the_data_with_reference_to_selected_options($self); 
					 if (defined @{$self->{missions_found}}[0]) 
					     {
					       $self->{mission} = @{$self->{missions_found}}[0]; print GLOBAL_LOG "  * loaded missions |@{$self->{missions_found}}|\n";
						   $self->{mission_menu}->configure( -choices  => \@{$self->{missions_found}}, );
					     }  
					 })-> pack(-side   =>'right', -expand => 0, -fill =>  'none',);	
    $self->{stb_button} = $frame_A->Checkbutton(-text     => 'ShowText',-onvalue  => 1,-offvalue => 0, -variable => \$self->{show_text_more},
						  -command  => sub
						  {
						  	return unless ($self->{initializatorator} > 0);
			                &sort_out_the_data_with_reference_to_selected_options($self);
						  }
						 )->pack(-side   => 'right',);
    $self->{write_codes_info}     = 1;
    $self->{scb_button} = $frame_A->Checkbutton(-text     => 'ShowCode',-onvalue  => 1,-offvalue => 0,-variable => \$self->{write_codes_info},
						  -command  => sub
						  {
						  	return unless ($self->{initializatorator} > 0);
			                &sort_out_the_data_with_reference_to_selected_options($self);
						  }
						 )->pack(-side   => 'right',);	

    $frame_A->Button( -text  => "...OpenFile", -relief  => 'groove',-command => sub {$self->{OPERATION} = 'open'; 
	                 &fileDialog($self);  unless(defined $self->{PROFILE_FILE}) {return;}; $self->{steady_code_selected} = 'ALL'; 
	                 &sort_out_the_data_with_reference_to_selected_options($self); 
					 if (defined @{$self->{missions_found}}[0]) 
					     {
						   $self->{added_file} = 1;
					       $self->{mission} = @{$self->{missions_found}}[0]; print GLOBAL_LOG "  * loaded missions |@{$self->{missions_found}}|\n";
						   $self->{mission_menu}->configure( -choices  => \@{$self->{missions_found}}, );
					     }  
					 })-> pack(-side   =>'right', -expand => 0, -fill =>  'none',);
					 

    @{$self->{pos_incre_level_required}}  = ('5','4','3','2','1');
    $self->{upto_incre_level}      =   5; # incre level is 1 to 5 position
    $self->{pos_incre_menu} =  $frame_A->Optionmenu(
			 -options      => [@{$self->{pos_incre_level_required}}],
			 -textvariable => \$self->{upto_incre_level},
			 -variable     => \$self->{upto_incre_level},
			 -justify      => 'right',-relief  => 'groove',-width        => 8,
			 -command      => sub
			                   {
			                      return unless ($self->{initializatorator} > 0);
			                       &sort_out_the_data_with_reference_to_selected_options($self);
			                   },)->pack(-side   => 'right', );	
							   
    @{$self->{steady_codes_found}} =  ('CODES');
	$self->{steady_code_selected}  = 'ALL';		
    $self->{segments_menu} =  $frame_A->BrowseEntry( -relief => 'sunken', -label => sprintf("%-5s","CODES"),-variable => \$self->{steady_code_selected}, -width    => 6, -justify  => 'right',
	                         -command      => sub
			                          {
			                            return unless ($self->{initializatorator} > 0);
			                            &sort_out_the_data_with_reference_to_selected_options($self);
			                         }, )->pack(-side   => 'right',);

    @{$self->{missions_found}}     = ('MISSION'); 
	$self->{mission}               =   1;
    $self->{mission_menu} =  $frame_A->BrowseEntry( -relief  => 'sunken',-label    => sprintf("%-8s","MISSION"),-variable => \$self->{mission}, -width    => 6, -justify  => 'right',
	                         -command      => sub
			                          {
			                            return unless ($self->{initializatorator} > 0);
			                            &sort_out_the_data_with_reference_to_selected_options($self);
			                         }, )->pack(-side   => 'right',);	
									 
    @{$self->{array_plot_PN_side}} = ('ALL','UP','DOWN'); 
    $self->{plot_pos_neg_side}	   = 'ALL';
    $self->{up_down_menu} =  $frame_A->BrowseEntry( -relief  => 'sunken',-label    => sprintf("%-8s","UP-DOWN"),-variable => \$self->{plot_pos_neg_side}, -width    => 6, -justify  => 'right',
	                         -command      => sub
			                          {
			                            return unless ($self->{initializatorator} > 0);
			                            &sort_out_the_data_with_reference_to_selected_options($self);
			                         }, )->pack(-side   => 'right',);
    my $info_label = $frame_C->Label(
	                -width => 60, 
				    -textvariable =>\$self->{INFO},
				    -relief => "sunken", 
					  -bd => 1, 
					  -anchor => 'w'
				    )->pack(
					    -side   =>'bottom',
                                -expand => 0,
			                    -fill   =>'x'
					   );									 
    $self->{initializatorator}    = 1;				   
   }


sub build_canvas_object_only
   {
    my $self            = shift;
	#$self->{added_file} = 1;
    if (exists $self->{canvas_frame}) {$self->{canvas_frame}->destroy;delete $self->{canvas_frame}; }
	$self->{canvas_frame}  = $self->{frame_B}->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'both',);
    $self->{CANVAS} = 
	$self->{canvas_frame}->Scrolled(
			'Canvas',
			-takefocus        => 1,
			-scrollbars       => 'se',
			-closeenough      => "2.0",
			-relief           => 'groove',
			-selectforeground => 'red',
			-selectbackground => 'blue',
			-cursor           => 'hand1',
			-background       => 'white',
			-width  => '850', 
			-height => '500',
			-borderwidth      =>  5
		       )->pack(
			       -side   => 'top',
			       -fill   => 'both',
			       -expand => 1,
			      );   
    $self->{CANVAS}->CanvasBind('<2>' => sub {my ($c) = @_;my $e = $c->XEvent;$c->scan('mark', $e->x, $e->y);});
    $self->{CANVAS}->CanvasBind('<B2-Motion>' => sub {  my ($c) = @_;my $e = $c->XEvent; $c->scan('dragto', $e->x, $e->y);});

    #    my ($l, $r, $t, $b) = $canvas->bbox('all');
    #    $canvas->configure(-scrollregion => ["$l", "$r", "$t", "$b"]);
    #    $canvas->CanvasBind("<Button-1>", sub{$self->printxy($self->{CANVAS},Ev('x'), Ev('y'))});
    my %pinfo;		
    $pinfo{'lastX'} = 0;
    $pinfo{'lastY'} = 0;
    $pinfo{'areaX2'} = -1;
    $pinfo{'prcmd'} = 'lp -dcldraco';
    $self->{CANVAS}->CanvasBind('<3>' => [sub {area_down(@_)}, \%pinfo]);
    $self->{CANVAS}->CanvasBind('<B3-Motion>' => [sub {area_move(@_)}, \%pinfo]);
	
    sub area_down
      {
	my($w, $pinfo) = @_;
	my $e = $w->XEvent;
	my($x, $y) = ($e->x, $e->y);
	$pinfo->{'areaX1'} = $x;
	$pinfo->{'areaY1'} = $y;
	$pinfo->{'areaX2'} = -1;
	$pinfo->{'areaY2'} = -1;
	eval {local $SIG{'__DIE__'}; $w->delete('area');};
      }

    sub area_move 
      {
	my($w, $pinfo) = @_;
	my $e = $w->XEvent;
	my($x, $y) = ($e->x, $e->y);
	if($x != $pinfo->{'areaX1'} && $y != $pinfo->{'areaY1'}) 
	  {
	    eval {local $SIG{'__DIE__'}; $w->delete('area');};
	    $w->addtag('area','withtag',$w->createRectangle($pinfo->{'areaX1'},$pinfo->{'areaY1'},$x,$y));
	    $pinfo->{'areaX2'} = $x;
	    $pinfo->{'areaY2'} = $y;
	  }
      }      
   } # end canvas
###################################  UTILITIES #####################################################   
   
sub fileDialog 
  {
    my $self  = shift;
    my $frame = $self->{MW};
    my $operation = $self->{OPERATION};
    my @types;

    #   Type names		Extension(s)	Mac File Type(s)
    #---------------------------------------------------------
    @types =
      (
       ["PURE files",           [qw/.profile .pure/]],
       ["Text files",           [qw/.txt .doc/]],
       ["Perl Scripts",         '.pl',          'TEXT'],
       ["All files",		'*']
      );
    my $File = $self->{PROFILE_FILE};
    if ($operation =~m/open/){$self->{PROFILE_FILE} = $frame->getOpenFile(-filetypes => \@types);}
    else {$self->{PROFILE_FILE} = $frame->getSaveFile(-filetypes => \@types,-initialfile => 'new',-defaultextension => '*.*');}
    if (defined $self->{PROFILE_FILE} and $self->{PROFILE_FILE} ne '') {return;}
    $self->{PROFILE_FILE} = $File;
  }

   
sub fileDialog_2
  {
    my $self  = shift;
    my $frame = $self->{MW};
    my $operation = $self->{OPERATION};
    my @types;

    #   Type names		Extension(s)	Mac File Type(s)
    #---------------------------------------------------------
    @types =
      (
       ["PURE files",           [qw/.profile .pure/]],
       ["Text files",           [qw/.txt .doc/]],
       ["Perl Scripts",         '.pl',          'TEXT'],
       ["All files",		'*']
      );
    my $File = $self->{PROFILE_FILE_2};
    if ($operation =~m/open/){$self->{PROFILE_FILE_2} = $frame->getOpenFile(-filetypes => \@types);}
    else {$self->{PROFILE_FILE_2} = $frame->getSaveFile(-filetypes => \@types,-initialfile => 'new',-defaultextension => '*.*');}
    if (defined $self->{PROFILE_FILE_2} and $self->{PROFILE_FILE_2} ne '') {return;}
    $self->{PROFILE_FILE_2} = $File;
  }  
  
  
  
sub activate_text_editor
  {
    my $self  = shift;
    my $top   = $self->{MW}->Toplevel();
    $top->title("$self->{PROFILE_FILE}");
    my $text = $top->Scrolled("Text",-width   => 60, -height => 25,-scrollbars => 'se',-cursor     => 'pirate',-wrap => 'none',)->pack(-side => 'top',-fill => 'both', -expand => 1, );
    open(REPORT,  "<". $self->{PROFILE_FILE});
    while (<REPORT>) {$text->insert('end', "$_");}
    close(REPORT);
  }

sub welcome_initialize
   {
     my $self = shift;
	 my @b    = ('300','250','400','350',);
	 my $text = 'Input File Required ---> *PROFILE [MaxMeanMin Extremes]';
    $self->{CANVAS}->createText( 500, 400,-text   => $text,-font   => 'Courier 20 bold',-fill   => 'red',-stipple => 'gray50', -tags    => 'item',); 
    $self->{CANVAS}->createOval("@b",-width    => '3m',-outline  => 'red',-tags    => 'item',) ;
    $self->{CANVAS}->createRectangle("@b", -width    => '1m', -outline  => 'blue',-tags    => 'item',);
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


#print GLOBAL_LOG "##   This program Plots the Extremes MaxMeanMin Segments of a Mission     \n";
#print GLOBAL_LOG "## --------------->>> Input is: *.profile file <<<--------------- \n";
#print GLOBAL_LOG "## ----->>> .......... AutoMulti-Step Version ................... \n";
#print GLOBAL_LOG "## ----->>> ............ with Sel. 1g only .......................\n";

















1;