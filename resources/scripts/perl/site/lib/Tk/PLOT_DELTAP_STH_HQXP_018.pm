package Tk::PLOT_DELTAP_STH_HQXP_018;

use strict;             # This PacKage PLOTs DELTAP or STH Files from Validation Process! Part of Fatigue Spectra Validation Suite!
use POSIX qw(acos sin cos ceil floor log10 atan);  # Derived from Tool = Plot_DeltaP_or_STH_Multiple_Profile_Flight_Visual
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
    my $self               = shift;
	$self->{tool_version}  = '1.6';
	
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
	
	$self->{delta_ignore}             = 0.000;                    # rforth value (ignore absolute delta with this value for plots)
	$self->{plot_all_pure_data}       =     1;                    # Yes is 1;   No is 0;
	$self->{flights_per_page}         = $root->{PLOT_OPTIONS}->{flights_per_page};
	$self->{load_flights_up2num}      = $root->{PLOT_OPTIONS}->{load_flights_up2num};
	$self->{show_text_more}           = $root->{PLOT_OPTIONS}->{show_text_more};
	$self->{do_fixed_scale_x}         = $root->{PLOT_OPTIONS}->{do_fixed_scale_x};
	$self->{force_scale_x_to}         = $root->{PLOT_OPTIONS}->{force_scale_x_to};
	$self->{do_fixed_scale_y}         = $root->{PLOT_OPTIONS}->{do_fixed_scale_y};
	$self->{force_scale_y_to}         = $root->{PLOT_OPTIONS}->{force_scale_y_to};
	$self->{shift_Y_text_to}          = $root->{PLOT_OPTIONS}->{shift_Y_text_to};            # '100';  # 425 
	$self->{neutral_axis_y_shift}     = $root->{PLOT_OPTIONS}->{neutral_axis_y_shift};       # 410   290 orig  330 370 410 (steps of 40)
	$self->{line_thickness}           =  0.4;                     # Default use 0.2 !
	print GLOBAL_LOG "  * |$self->{directory_path}|\n";
	
    $self->{DELTAP_FILE}              = $root->{MISSION}->{$wmai}->{ANA_FILE} . '.all.deltap';
	$self->{eid}                      = $root->{MISSION}->{$wmai}->{STF_FILE};
	if ($self->{eid} =~m/\..+$/i) {$self->{eid} =~s/\..+//;} else {$self->{eid} = $self->{eid} . '_x';}	
	$self->{STH_FILE}                 = $self->{eid} .  '_HQ.sth';

    print GLOBAL_LOG "\n  * <<START>> PLOT DELTAP or STH Process  |$wmai|$self->{DELTAP_FILE}|$self->{STH_FILE}|\n\n";
	unless ((-e $self->{DELTAP_FILE})  && (-e $self->{STH_FILE})) {print GLOBAL_LOG " *** Not all Files Exists!\n"; return;}
    $self->Start_PLOT_Deltap_STH_process($root);
	print GLOBAL_LOG "\n  * <<END>>   Completed Plot Process\n";
	close(GLOBAL_LOG);
  }




sub Start_PLOT_Deltap_STH_process
  {
     my $self = shift;
	 my $root = shift;

	 # DO NOT EDIT BELOW THIS LINE
	 $self->{error} = 0;
	 if    ($self->{PLOT} =~m/PLOT/) {$self->{PROF_FILE} = $self->{DELTAP_FILE}; &comb_prof_file_for_interesting_dp_profiles($self);}  #'A350XWB900-528NF-SR3.ana.all.deltap'      'rib26_test2_T2.sth'
	 elsif ($self->{PLOT} =~m/STH/)  {$self->{PROF_FILE} = $self->{STH_FILE};    &comb_STH_file_for_requested_flights($self);}	 
	 
	 unless ($self->{error} > 0) 
	    {
	      print STDERR "\n      ...... please wait ......   \n";
	      &build_Tk_frame_structure($self);	  
		  $self->{combined_report_file} = $self->{PROF_FILE} . '_' . $self->{PLOT} . '.all.ps';   open (OUT, ">" . $self->{combined_report_file}); close(OUT);
		  my $pages = ceil($self->{load_flights_up2num} / $self->{flights_per_page}); 
		  for (1 .. $pages)
		     {
		       $self->{all_menu_button}->cget(-menu)->invoke('Gen PostS');     #       cget(-menu)->invoke('View Results');
		       $self->{next_page_button}->invoke();
		     }
	    }
     #&activate_objscan($self);  
     #MainLoop;		   #  ACTIVATE this LINE to see the TK Object Plotted!
     $self->{MW}->destroy() if Tk::Exists($self->{MW});	
  }






sub build_Tk_frame_structure
  {
    my $self         = shift;
	
    my $mw     = MainWindow->new();
    $mw->title("Plot Max & Min Flights!  version  1.000");
    $mw->optionAdd('*font' => 'Courier 10');
    $mw->geometry("1000x600+50+50");		
    $self->{MW} =  $mw;

    my $frame_A       = $mw->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'x',);
    $self->{frame_B}  = $mw->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'both',);	 

    my $graph_Menu = $self->{all_menu_button}  = 
      $self->{MENU} = $frame_A->Menubutton(
						      #-image       => $image,
						      -text   => 'View',
						      -underline   => 0,
						      -tearoff     => 0,
						     )->pack(
							     -side => 'left',
							    );

    my $zoom_in = $graph_Menu->radiobutton(
					   -label     => 'Zoom ++',
					   -value    => 'ZOOM',
					   -command   => sub 
					   { 
					     my ($l, $r, $t, $b) = $self->{CANVAS}->bbox('all');
					     $self->{CANVAS}->scale("all", "$l","$r", 2.0, 2.0);
					   },);

    my $zoomout = $graph_Menu->radiobutton(
					   -label     => 'Zoom --',
					   -value    => 'OUT',
					   -command   => sub 
					   { 
					     my ($l, $r, $t, $b) = $self->{CANVAS}->bbox('all');
					     $self->{CANVAS}->scale("all", "$l","$r", 0.5, 0.5);
					   },);
    $self->{page_num} = 0;
    $self->{postscript_menu_button} = $graph_Menu->radiobutton(                     #   radiobutton  instead of command
					   -label     => 'Gen PostS',
					   -value     => 'PS',
					   -command   => sub 
					   {
                         $self->{page_num}++;
					     my $file_1 = $self->{PROF_FILE} . '_' . $self->{page_num} . '.ps';
					     $self->{CANVAS}->postscript(
								 -file      => "$file_1",
								 -colormode => "color",
								 -rotate    => 1,
								 -width  => '1000',
								 -height => '600',
								 -pagewidth  => '1000',
								 -pageheight => '600',
								 #-pagex      => '100',
								 #-pagey      => '100',
								 -pageanchor => 'center',
								);
						 open (INP, "<" . $file_1);
                         my @array = <INP>;
                         close(INP);
                         open (OUT, ">>" . $self->{combined_report_file});
                         print OUT "@array\n";
                         close(OUT);						 
					   },);
					   
    $self->{initiator} = 0;
    my $button_next = $self->{next_page_button} = 
	    $frame_A->Button(
                     -text  => ">>>>> NEXT >>>>>", 
					 -relief  => 'groove',
					 -command => sub {
					                  if (exists $self->{canvas_frame}) {$self->{canvas_frame}->destroy;delete $self->{canvas_frame}; }
					                   $self->{initiator}++;
									   $self->{plot_from_this_num}   = $self->{flights_per_page} * $self->{initiator} - $self->{flights_per_page} + 1;
                                       $self->{plot_to_this_num}     = $self->{flights_per_page} * $self->{initiator};
			
                                       &build_Tk_objects_DP_profile_SERIES($self);
									   #&activate_objscan($self);  
					                 }
					)-> pack(-side   =>'top', -expand => 0, -fill =>  'none',);					   
	$button_next->invoke();				   
  } # end subroutine structure
  
  
  
sub build_Tk_objects_DP_profile_SERIES      # One after the Other!
  {
    my $self         = shift;
    my $y_text_shift = $self->{shift_Y_text_to}; # '100';  # 425  	
    my @colours = qw/yellow blue gold green black red cyan purple orange grey brown violet yellow pink gold white red green AliceBlue Aquamarine DarkGoldenrod DarkKhaki Coral DarkOrange DarkOrchid1 DarkSeaGreen DarkSlateBlue DarkViolet DeepPink DeepSkyBlue DimGray Firebrick GreenYellow Goldenrod HotPink IndianRed LawnGreen LightBlue LightCyan LightGray LightSalmon LightSkyBlue LightYellow Magenta LimeGreen Maroon MintCream MistyRose OrangeRed Orchid PaleGreen PaleGoldenrod PapayaWhip PeachPuff Plum PowderBlue RosyBrown Salmon SandyBrown SeaGreen Sienna SkyBlue Thistle VioletRed azure beige bisque burlywood chartreuse chocolate yellow blue black cyan purple orange grey brown violet yellow pink gold white red green AliceBlue Aquamarine DarkGoldenrod DarkKhaki Coral DarkOrange DarkOrchid1 DarkSeaGreen DarkSlateBlue DarkViolet DeepPink DeepSkyBlue DimGray Firebrick GreenYellow Goldenrod HotPink IndianRed LawnGreen LightBlue LightCyan LightGray LightSalmon LightSkyBlue LightYellow Magenta LimeGreen Maroon MintCream MistyRose OrangeRed Orchid PaleGreen PaleGoldenrod PapayaWhip PeachPuff Plum PowderBlue RosyBrown Salmon SandyBrown SeaGreen Sienna SkyBlue Thistle VioletRed azure beige bisque burlywood chartreuse chocolate/;	

	if ($self->{total_flights_seen} < $self->{plot_to_this_num}) {$self->{plot_to_this_num} = $self->{total_flights_seen}; $self->{plot_from_this_num} = $self->{total_flights_seen} - $self->{flights_per_page} + 1;}	
    ##### Find maximum DATA values
	&solve_for_x_axis_peaks($self);
    my $max_y   = $self->{value_max};  if (abs($self->{value_min}) > $max_y) {$max_y = abs($self->{value_min});}
	my $max_x   = $self->{total_points_accu_x}; #print GLOBAL_LOG "      * Max Points in X: $max_x\n";
	if ($max_x < 1) {print GLOBAL_LOG "    ** All data to be plotted are Zero - check if this CDF should have Values > 0 ?"; return;}
    ##### Multipl. factors for graph
    my $scale_y = 1;
    my $scale_x = 1;
	
	if   ($max_x    <=  16) {$scale_x = 4.0;} # Number of Codes X-dir
	elsif($max_x    <=  31) {$scale_x = 2.0;}
	elsif($max_x    <=  46) {$scale_x = 1.5;}	
	elsif($max_x    <=  61) {$scale_x = 1.0;}
	elsif($max_x    <=  91) {$scale_x = 0.75;}	
	elsif($max_x    <= 121) {$scale_x = 0.50;}
	elsif($max_x    <= 240)   {$scale_x = 0.25;}
	elsif($max_x    <= 480)   {$scale_x = 0.125;}	
	elsif($max_x    <= 960)   {$scale_x = 0.0625;}	
	elsif($max_x    <= 2000)  {$scale_x = 0.03125;}	
	elsif($max_x    <= 4000)  {$scale_x = 0.015625;}
	elsif($max_x    <= 6000)  {$scale_x = 0.0104166;}	
	elsif($max_x    <= 8000)  {$scale_x = 0.0078125;}	
	elsif($max_x    <= 12000) {$scale_x = 0.00520833;}
	elsif($max_x    <= 14000) {$scale_x = 0.00455729;}
	elsif($max_x    <= 16000) {$scale_x = 0.00390625;}
	elsif($max_x    <= 24000) {$scale_x = 0.002604166;}	
	elsif($max_x    <= 32000) {$scale_x = 0.001953125;}	
    else {$scale_x   = 0.0009765625;}

	$scale_x =  60 / $max_x;
	$scale_x = 1.5 * $scale_x;  # Expand by ratio 30/20 lines
	if ($self->{do_fixed_scale_x} =~m/yes/i) {$scale_x = $self->{force_scale_x_to};}
	
	if   (($max_y   >  800) && ($max_y   < 1200))      {$scale_y = 0.25;} # Stress / Load Value Y-dir
	elsif(($max_y   >  350) && ($max_y   <  800))      {$scale_y = 0.5;} # Stress / Load Value Y-dir	
	elsif(($max_y   >  125) && ($max_y   <  350))      {$scale_y = 1.0;}
	elsif(($max_y   > 62.5) && ($max_y   <  125))      {$scale_y = 2.0;}
    elsif(($max_y   > 31.25) && ($max_y   <  62.5))    {$scale_y = 4.0;}
    elsif(($max_y   > 15.625) && ($max_y   <  31.25))  {$scale_y = 8.0;}
    elsif(($max_y   > 7.8125) && ($max_y   <  15.625)) {$scale_y = 16.0;} 
    elsif(($max_y   > 3.91) && ($max_y   <  7.8125))   {$scale_y = 32.0;} 
    elsif(($max_y   > 1.95) && ($max_y   <  3.91))     {$scale_y = 64.0;}  
    else {$scale_y   = 128.0;}
	
	if ($self->{do_fixed_scale_y} =~m/yes/i) {$scale_y = $self->{force_scale_y_to};}
	
    #Shift Y direction
    my $shy = $self->{neutral_axis_y_shift} ; # 290;	
    ##############################################
	$self->{canvas_frame}  = $self->{frame_B}->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'both',);
    my $canvas = 
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
			-width  => '1000', 
			-height => '600',
			-borderwidth      =>  5
		       )->pack(
			       -side   => 'top',
			       -fill   => 'both',
			       -expand => 1,
			      );
    ##### DRAW PLOT
    my $tc         = 'brown';
    my $at         = 'black';
	my $plot_font  = 'Courier 8';
	my $thick_font = 'Courier 8 bold';
	my $thick_more = 'Courier 12 bold';	
    my $mini_font  = 'Courier 6 bold';
    my $titlefont  = 'Courier 10 italic';

    ##### AXIS created here
    $canvas->createLine(50, $shy, 950, $shy, -width => 2);  # X
    $canvas->createLine(50, 450,  50,  50,   -width => 2);  # Y
    $canvas->createText(
			450, 25, 
			-text => "View of DeltaP / STH Profile [rfort: $self->{delta_ignore}] //File: $self->{PROF_FILE}//", 
			-font => $titlefont,
			-fill => $tc,
		       );

    ##### Vertical GRIDS created here
    for (1 .. 30)
      {
	    my $x = 50 + ($_ * 30);
	    $canvas->createLine($x, 450, $x, 50, -width => 1, -fill => 'white');
      }

    ##### Horizontal GRIDS created here
    for (0 .. 10)
      {
	my $y =  450 - ($_ * 40);
	$canvas->createLine(50, $y, 950, $y, -width => 1, -fill => 'brown');
      }

    ##### MARKS on X-AXIS & Y-AXIS  created here
    my($i, $x, $y, $point, $item);

    for ($i = -1; $i <= 16; $i++)   # Labels Values on Y axis 
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
	  
	### WRITE X-axis VALUES 
	my $divisor = 10 ** (length($max_x) - 1); 
    my $xpd = ceil($max_x/$divisor); #print STDERR "|$xpd|$divisor\n";
	for (1 .. $xpd) 
	 {
	   my $num =  $_ * $divisor;
       my $x   =  50 + ($num * 10) * $scale_x; 
	   $canvas->createLine($x, 405, $x, 415, -width => 1);
 	   $canvas->createText($x, $y_text_shift,-text => $num, -anchor => 'n',-font   => $plot_font);
	 }
		  
    # WRITE LEGENDS
    my $x_legend = $canvas->createText(
				       475, 460, # X, Y
				       -text => "acc. Peaks",
				       -font => $thick_font,
				       -fill => $at,
				      ); 
    my $y_legend = $canvas->createText(
				       80, 40, 
				       -text => " [mpa]or[mbar]", 
				       -font => $thick_font,
				       -fill => $at,
				      );
    my $Pure_legend = $canvas->createText(
				       120, 40, # X, Y
				       -text    => "EPuRE", 
				       -anchor  => 'e',
				       -font    => 'Courier 10 bold',
				       -fill    => 'pink',
				       -stipple => 'gray50',
				       -tags    => 'item',					   
				      ); 					  
				  
	# X, Y
    my $ftse = $self->{load_flights_up2num};
    if (defined $self->{yes_we_saw_all}) { $ftse = 'ALL';}
	my $max_text = sprintf("Max %8.2f [FTs:%3s]", "$self->{value_max}","$ftse");
	my $min_text = sprintf("Min %8.2f [FTs:%3s]", "$self->{value_min}","$ftse");
	$canvas->createText(250, 60, -text => $max_text, -font => $thick_font, -fill => $at,); 
	$canvas->createText(250, 70, -text => $min_text, -font => $thick_font, -fill => $at,);
	
    my $j  = 0; my $x_p  = 'X'; my $m_p = 'M'; my $k = 0;
	my $x_now = 50;
    my $farbe =  0; my $ypnt = 0;
	if ($self->{plot_from_this_num} > 0)  # MULTIPLE FLIGHTS
	  { 
	   my @y = ('80','90','80','90','80','90','80','90','80','90','80');
	   for ($self->{plot_from_this_num} .. $self->{plot_to_this_num}) 	   #foreach(sort @{$self->{ftnum_plot_order}})
	     {
		   my $f_num  = $_; $farbe++;   $ypnt++;
		   my $header = unpack('A12', $self->{HEADERS}->{$f_num});
	       my @array  = @{$self->{TF_DATA}->{$f_num}};
           my $colour = $colours[$farbe];	last if (!defined $colour);
           if ($farbe > 10)  {$farbe = 1;}
           if ($ypnt  > 10)  {$ypnt  = 1;}		   
		   my $yns = 50 + 10*$_;  my $text = '[' . $_ .']';
		   $canvas->createText($x_now,    $y[$ypnt], -text => $text,   -font => $thick_font, -fill => $colour,-anchor => 'w',); 
		   $canvas->createText($x_now+30, $y[$ypnt], -text => $header, -font => $thick_more, -fill => $colour,-anchor => 'w',) if ($self->{show_text_more} > 0);
           $canvas->createLine($x_now, 400, $x_now,  100, -dash => '-', -fill => 'gold',  -width => 0.2);			   
		   $j = 0; $x_p = 'X'; $m_p = 'M'; $k = 0;

		   my $max_i = sprintf("%6.2f", "$self->{value_max_single}->{$f_num}");
	       my $min_i = sprintf("%6.2f", "$self->{value_min_single}->{$f_num}");
	       $canvas->createText($x_now+30, $y[$ypnt]+10, -text => $max_i, -font => $thick_font, -fill => $at,); 
	       $canvas->createText($x_now+30, $y[$ypnt]+20, -text => $min_i, -font => $thick_font, -fill => $at,);

		   foreach (@array)
            {
		      if ($_ =~m/^\s*$/) {next;}; 
			  #if ($_ < 0)   {}; # just a number check
	          my $value = $_;
		
		      $j++; $k++;
		      my $x   = $x_now + ($j * 10)  * $scale_x;
		      my $m   = $shy - (4 * $value) * $scale_y / 5;
		
		      if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m;}
		      $canvas->createLine($x_p, $m_p, $x,  $m, -fill => $colour,  -width => "$self->{line_thickness}");
		      $x_p  = $x;
		      $m_p  = $m;
            }
		   $x_now  = $x_p;
	     }	  
	  } # end Multiple	  
   #print GLOBAL_LOG "      * scale X: $scale_x \n";  
  } # end of MAiN

  
  

sub solve_for_x_axis_peaks
   {
      my $self  = shift;
	  $self->{total_points_accu_x} = 0;
	  
	  for ($self->{plot_from_this_num} .. $self->{plot_to_this_num})
	    {
		  
		  $self->{total_points_accu_x} = $self->{total_points_accu_x} + $#{$self->{TF_DATA}->{$_}};
		}
   }
 
  
  
sub comb_prof_file_for_interesting_dp_profiles
  {
    my $self      = shift;
	my $num       = 0;

    open (INPUT,  "< $self->{PROF_FILE}");
	$self->{value_max}   = -1e9;
    $self->{value_min}   = +1e9;
	my $header;	
	my $valp    = 0;  
	my $value   = 0;
    my $code_1g = 0;
	my $j    = 0;
    while (<INPUT>)
      {
	    last if eof;
	    if (($_ =~m/^\s*$/) || ($_ =~m/^\s*\#/)) {print GLOBAL_LOG "$_\n";next;}
		my $line = $_; $line =~s/^\s*//;
        chomp($line);
		if ($line =~m/FLIGHT/i)  
		  {
		    $num++;  
			my ($a, $n, $header) = split('\s+', $line); 
			$valp = 0;
			unless ($n == $num) {print GLOBAL_LOG "  *** Error in Data Sequence\n";};
			$self->{HEADERS}->{$num} = $header; 
			next;
		  }
        $j++;
		#($value, $code_1g) = split('\s+', $line);
		$value    = $line;
		if ($self->{plot_all_pure_data} < 1) {my $delta = abs($value - $valp); if (($delta <= $self->{delta_ignore}) && ($j > 1)) {next;}}
		if (($value == 0) && ($j > 1)) {next;}
		$valp = $value;
		push(@{$self->{TF_DATA}->{$num}}, $value)    if (defined $value);   
        #push(@{$self->{GG_DATA}->{$num}}, $code_1g) if (defined $code_1g);  
        if ($value > $self->{value_max}) {$self->{value_max} = $value;}
		if ($value < $self->{value_min}) {$self->{value_min} = $value;}
		
		unless(defined $self->{value_max_single}->{$num}) {$self->{value_max_single}->{$num} = -1e9; $self->{value_min_single}->{$num} = +1e9;}
		if ($value > $self->{value_max_single}->{$num}) {$self->{value_max_single}->{$num} = $value;}
		if ($value < $self->{value_min_single}->{$num}) {$self->{value_min_single}->{$num} = $value;}	
		
		last if ($num > $self->{load_flights_up2num});  # just for a few flights
      }
	$self->{total_flights_seen} = $num;  
	#@dp_array = ();
	if ((abs($self->{value_max}) < 0.1) && (abs($self->{value_min}) < 0.1)) {$self->{error} = 1; print GLOBAL_LOG "   *** The requested PLOT < $self->{PLOT} > has all ZERO Values!\n";}
	if ($self->{load_flights_up2num} > $self->{total_flights_seen}) {$self->{yes_we_saw_all} = 'ALL';}
	close(INPUT);
  } # 



sub comb_STH_file_for_requested_flights
  {
    my $self = shift;
    my @header = ();
    my @names  = ();
    my ($validfor, $block, $name, $points);
    my $column        = 8.0;
    my $sum           =   0;
    my ($line, $rows);
    my $i             = 0;
	$self->{value_max}   = -1e9;
    $self->{value_min}   = +1e9;
    my ($max_ftype,$min_ftype);
	
    open (INPUT,  "< $self->{PROF_FILE}");	
    foreach (1 .. 4)
      {
	    $line = <INPUT>;
	    push(@header, $line);
      }

    for(1 .. 20001) # Max FT number plotted
      {
		last if eof;
		$line     = <INPUT>;
		chomp($line);
		$line     =~ s/^\s*//;
		if (($line =~m/^\s*$/) || ($line =~m/^\s*\#/)) {push(@header, $line);next;}
		$i++;
		
		my @a = split(/\s+/, $line);
		if (defined $a[2]) {($validfor, $block, $name) = split(/\s+/, $line);}
		else {($validfor, $block) = split(/\s+/, $line); $name  = 'TF_' . $i; }

		$line     = <INPUT>;
		chomp($line);
		$line     =~ s/^\s*//;

		my @b = split(/\s+/, $line);
		if (defined $b[1]) {($points, $name) = split(/\s+/, $line);}
		else {($points) = split(/\s+/, $line);}
		$points =~s/\s*//;  $self->{HEADERS}->{$i} = $name;
		push(@names, $name);
		#print STDERR "|$name|";

		$rows     = $points / $column;
		my $int   = floor($rows); # gives the Interger part of Number
		my $dig   = ceil($rows);  # makes 30.25 to become 31
		$rows     = $dig;
		
		my @array = ();
		for (1 .. $rows) { $line = <INPUT>;  push(@array, $line); } 

		foreach (@array)
		  {
		    chomp($_);
		    my @block = split(/\s+/,$_);
		    foreach (@block)
		      {
			    unless ($_ =~m/^\s*$/)
			      {	
			        my $value = $_;
					if ($value > $self->{value_max}) {$self->{value_max} = $value;}
		            if ($value < $self->{value_min}) {$self->{value_min} = $value;}
					unless(defined $self->{value_max_single}->{$i}) {$self->{value_max_single}->{$i} = -1e9; $self->{value_min_single}->{$i} = +1e9;}
					if ($value > $self->{value_max_single}->{$i}) {$self->{value_max_single}->{$i} = $value;}
		            if ($value < $self->{value_min_single}->{$i}) {$self->{value_min_single}->{$i} = $value;}					
					push (@{$self->{TF_DATA}->{$i}}, $_);
			      }
		      }
		  }
		 				  
		$sum = $sum + $validfor * $block;
		#print STDERR sprintf("%4s %10s %s","$i","$name","\n");
		last if ($i > $self->{load_flights_up2num});  # just for a few flights
      }
    close(INPUT);
	$self->{total_flights_seen} = $i;
    if ($self->{load_flights_up2num} > $self->{total_flights_seen}) {$self->{yes_we_saw_all} = 'ALL';}	
  } # STH

  
  


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

  
 	 # Options
	 #$self->{delta_ignore}           = 0.000;                    # rforth value (ignore absolute delta with this value for plots)
	 #$self->{plot_all_pure_data}     =     1;                    # Yes is 1;   No is 0;
	 
	 #$self->{flights_per_page}       =     1;
	 #$self->{load_flights_up2num}    =     5;
	 #$self->{show_text_more}         =     1;
	 #$self->{do_fixed_scale_x}       =  'NO';
	 #$self->{force_scale_x_to}       =  0.25;
	 #$self->{do_fixed_scale_y}       =  'NO';
	 #$self->{force_scale_y_to}       =     2; 
  

#print STDERR "##       This program Plots the Delta Pressure Profile of each Flight!\n";  
#print STDERR "##  %%%%%The input file is a *.deltap file from Count Flugablauf Tool!%%%%%\n";
#print STDERR "##        ----->>> Input is the: *.deltap file <<<----- \n";
#print STDERR "##        ----->>> ....... <<<-----\n";









1;