package Tk::PLOT_STH_MAX_MIN_HQ_029;

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
	$self->{tool_version}  = '2.9';
	
    if (@_)
      {
        my %extra = @_;
        @$self{keys %extra} = values %extra;
      }

    my $root                          = $self->{ROOT};
	my $wmai                          = $self->{MY_MISSION};
	$self->{sth_or_sigma}             = $self->{PLOT};        # STH or SIGMA  where PLOT is Deltap!	
	
	open(GLOBAL_LOG, ">>". $root->{GLOBAL}->{GLOBAL_LOGFILE}); 	
	$self->{directory_path}           = $root->{GLOBAL}->{WORKING_DIRECTORY};    
    $self->{date}                     = $root->{GLOBAL}->{DATE};
	$self->{mission}                  = $root->{MISSION}->{$wmai}->{MISSION};
    $self->{overall_factor}           =  1.0;	
	$self->{delta_ignore}             = 0.000;                    # rforth value (ignore absolute delta with this value for plots)
	$self->{plot_PURE_data}           =     1;                    # Yes is 1;   No is 0;
    $self->{do_first_12_ft}           = 'YES'; 
	$self->{user_selected_ft}         =    10;
    $self->{plot_ONLY_this_FLIGHT}    =     0;
    $self->{plot_parralel_series}     = 'SERIES';
	$self->{do_fixed_scale_x}         = $root->{PLOT_OPTIONS}->{do_fixed_scale_x};
	$self->{force_scale_x_to}         = $root->{PLOT_OPTIONS}->{force_scale_x_to};
	$self->{do_fixed_scale_y}         = $root->{PLOT_OPTIONS}->{do_fixed_scale_y};
	$self->{force_scale_y_to}         = $root->{PLOT_OPTIONS}->{force_scale_y_to};
	$self->{shift_Y_text_to}          = $root->{PLOT_OPTIONS}->{shift_Y_text_to};            # '100';  # 425 
	$self->{neutral_axis_y_shift}     = $root->{PLOT_OPTIONS}->{neutral_axis_y_shift};       # 410   290 orig  330 370 410 (steps of 40)
	
	print GLOBAL_LOG "  * |$self->{directory_path}|\n";
	
	$self->{eid}                      = $root->{MISSION}->{$wmai}->{STF_FILE};
	if ($self->{eid} =~m/\..+$/i) {$self->{eid} =~s/\..+//;} else {$self->{eid} = $self->{eid} . '_x';}	
	$self->{STH_FILE}                 = $self->{eid} .  '_HQ.sth';

    print GLOBAL_LOG "\n  * <<START>> PLOT Flights MAX MIN PEAKs Process  |$wmai|$self->{STH_FILE}|\n\n";
	unless (-e $self->{STH_FILE}) {print GLOBAL_LOG " *** Not all Files Exists  |$self->{STH_FILE}| File not Found!\n"; return;}
    $self->Start_PLOT_STH_MAX_MIN_process($root);
	print GLOBAL_LOG "\n  * <<END>>   Completed Plot Max Min Process\n";
	close(GLOBAL_LOG);
  }

  
# DO NOT EDIT BELOW THIS LINE
sub Start_PLOT_STH_MAX_MIN_process
  {
     my $self = shift;
	 my $root = shift; 
	 $self->{error} = 0;
	 unless ($self->{plot_ONLY_this_FLIGHT} < 1) {$self->{plot_SINGLE_flight} = 'YES';} else {$self->{plot_SINGLE_flight} = 'NO';}

	 if    ($self->{PLOT} =~m/STH/i)   {&load_sth_file_write_grace($self);}  
	 elsif ($self->{PLOT} =~m/SIGMA/i) {&load_sigma_file_write_grace($self);}
     else {$self->{error} = 1; print GLOBAL_LOG " *** Sth or Sigma Key was not provided\n";}	 
	 
	 unless ($self->{error} > 0)
 	  {
 	    &build_Tk_objects_SERIES($self)     if ($self->{plot_parralel_series} =~m/SER/i);
 	    &build_Tk_objects_PARRALLEL($self)  if ($self->{plot_parralel_series} =~m/PAR/i);  # not implemented!
        $self->{all_menu_button}->cget(-menu)->invoke('Gen PostS'); 
      } 
     #&activate_objscan($self);  
     #MainLoop;		   #  ACTIVATE this LINE to see the TK Object Plotted!
     $self->{MW}->destroy() if Tk::Exists($self->{MW});		  
  }



sub load_sigma_file_write_grace
  {
    my $self             = shift;
	$self->{PLOT_FILE}   = $self->{STH_FILE} .'.ploted_maxmin.agr';
	$self->{PLOT_MORE}   = $self->{STH_FILE} .'.extraplot';	
    open (INPUT,  "< $self->{STH_FILE}");
    open (OUTPUT, "> $self->{PLOT_FILE}");
    open (OUTPUT2, "> $self->{PLOT_MORE}");	
    &comb_sigma_file_for_interesting_flights($self);
    &plot_the_min_max_data_files($self) unless ($self->{error} > 0);
    close(INPUT);
    close(OUTPUT);
	close(OUTPUT2);
  }


sub load_sth_file_write_grace
  {
    my $self             = shift;
	$self->{PLOT_FILE}   = $self->{STH_FILE} .'.ploted_maxmin.agr';
	$self->{PLOT_MORE}   = $self->{STH_FILE} .'.extraplot';		
    open (INPUT,  "< $self->{STH_FILE}");
    open (OUTPUT, "> $self->{PLOT_FILE}");
    open (OUTPUT2, "> $self->{PLOT_MORE}");		
    &comb_sth_file_for_interesting_flights($self);
    &plot_the_min_max_data_files($self) unless ($self->{error} > 0);
    close(INPUT);
    close(OUTPUT);
	close(OUTPUT2);	
  }

  
sub build_Tk_objects_SERIES      # One after the Other!
  {
    my $self         = shift;
    my $colour_max   = 'red';
    my $colour_min   = 'green';
    my $colour_long  = 'gold';
    my $colour_user  = 'black';     
    my $y_text_shift = $self->{shift_Y_text_to}; # '100';  # 425  	
    my @colours = qw/yellow blue black cyan purple orange grey brown violet yellow pink gold white red green AliceBlue Aquamarine DarkGoldenrod DarkKhaki Coral DarkOrange DarkOrchid1 DarkSeaGreen DarkSlateBlue DarkViolet DeepPink DeepSkyBlue DimGray Firebrick GreenYellow Goldenrod HotPink IndianRed LawnGreen LightBlue LightCyan LightGray LightSalmon LightSkyBlue LightYellow Magenta LimeGreen Maroon MintCream MistyRose OrangeRed Orchid PaleGreen PaleGoldenrod PapayaWhip PeachPuff Plum PowderBlue RosyBrown Salmon SandyBrown SeaGreen Sienna SkyBlue Thistle VioletRed azure beige bisque burlywood chartreuse chocolate yellow blue black cyan purple orange grey brown violet yellow pink gold white red green AliceBlue Aquamarine DarkGoldenrod DarkKhaki Coral DarkOrange DarkOrchid1 DarkSeaGreen DarkSlateBlue DarkViolet DeepPink DeepSkyBlue DimGray Firebrick GreenYellow Goldenrod HotPink IndianRed LawnGreen LightBlue LightCyan LightGray LightSalmon LightSkyBlue LightYellow Magenta LimeGreen Maroon MintCream MistyRose OrangeRed Orchid PaleGreen PaleGoldenrod PapayaWhip PeachPuff Plum PowderBlue RosyBrown Salmon SandyBrown SeaGreen Sienna SkyBlue Thistle VioletRed azure beige bisque burlywood chartreuse chocolate/;	
	
    ##### Find maximum DATA values
    my $max_y   = $self->{value_max};  if (abs($self->{value_min}) > $max_y) {$max_y = abs($self->{value_min});}
    my $max_x   = $self->{total_points_accu_x}; #print GLOBAL_LOG " Max Points in X: $max_x\n";
	
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
	if   (($max_y   > 1200) && ($max_y   < 2000))      {$scale_y = 0.25;}
	elsif(($max_y   >  500) && ($max_y   < 1200))      {$scale_y = 0.25;} # Stress / Load Value Y-dir
	elsif(($max_y   >  350) && ($max_y   <  500))      {$scale_y = 0.5;} # Stress / Load Value Y-dir	
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
	
    my $mw     = MainWindow->new();
    $mw->title("Plot Max & Min Flights!  version  1.000");
    $mw->optionAdd('*font' => 'Courier 10');
    $mw->geometry("1000x600+50+50");		
    $self->{MW} =  $mw;

    my $frame_A  = $mw->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'x',);
    my $frame_B  = $mw->Frame(-relief => 'groove', -borderwidth => 2)->pack(-side   =>'top', -fill   =>  'both',);
	
    my $canvas = 
    $self->{CANVAS} = 
	$frame_B->Scrolled(
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


    my $graph_Menu = $self->{all_menu_button} =
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
					     my ($l, $r, $t, $b) = $canvas->bbox('all');
					     $canvas->scale("all", "$l","$r", 2.0, 2.0);
					   },);

    my $zoomout = $graph_Menu->radiobutton(
					   -label     => 'Zoom --',
					   -value    => 'OUT',
					   -command   => sub 
					   { 
					     my ($l, $r, $t, $b) = $canvas->bbox('all');
					     $canvas->scale("all", "$l","$r", 0.5, 0.5);
					   },);

    my $get_image = $graph_Menu->radiobutton(
					     -label     => 'Cen Image',
					     -value    => 'GET',
					     -command   => sub
					     { 
					       my ($l, $r, $t, $b) = $canvas->bbox('all');
					       $canvas->configure(-scrollregion => ["$l", "$r", "$t", "$b"]);
					     },);

    my $fit_screen = $graph_Menu->radiobutton(
					      -label     => 'Fit Image',
					      -value    => 'FIT',
					      -command  => sub
					      { 
						my ($l, $r, $t, $b) = $canvas->bbox('all');
						my $cur_width  = $canvas->width();
						my $cur_height = $canvas->height();
						
						my $image_w = $t - $l;
						my $image_h = $b - $r;
						
						my $x_ratio = $cur_width/$image_w;
						my $y_ratio = $cur_height/$image_h;
						
						$canvas->scale("all", "$l","$r", "$x_ratio","$y_ratio");
						#$canvas->configure(-scrollregion => ["$l", "$r", "$t", "$b"]);
					      },);
			
    my %pinfo;		
    $pinfo{'lastX'} = 0;
    $pinfo{'lastY'} = 0;
    $pinfo{'areaX2'} = -1;
    $pinfo{'prcmd'} = 'lp -dcldraco';

    my $print_area = $graph_Menu->radiobutton(
					      -label     => 'Sel Print',
					      -value     => 'PA',
					      -command   => [\&area_save, $canvas, \%pinfo],
					     );

    my $postscr = $graph_Menu->radiobutton(
					   -label     => 'Gen PostS',
					   -value     => 'PS',
					   -command   => sub 
					   {
					     my $file_1 = $self->{STH_FILE} . '.series.ps';
					     $canvas->postscript(
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
					
					   },);

    $canvas->CanvasBind('<2>' => sub {
			  my ($c) = @_;
			  my $e = $c->XEvent;
			  $c->scan('mark', $e->x, $e->y);
			});

    $canvas->CanvasBind('<B2-Motion>' => sub {
			  my ($c) = @_;
			  my $e = $c->XEvent;
			  $c->scan('dragto', $e->x, $e->y);
			});

    ##### DRAW PLOT
    my $tc         = 'brown';
    my $at         = 'black';
    my $plot_font  = 'Courier 8';
    my $thick_font = 'Courier 8 bold';
    my $titlefont  = 'Courier 10 italic';

    ##### AXIS created here
    $canvas->createLine(50, $shy, 950, $shy, -width => 2);  # X
    $canvas->createLine(50, 450,  50,  50,   -width => 2);  # Y
    $canvas->createText(
			450, 25, 
			-text => "Compared MAX vs. MIN Flight [1/Block] Sequence [rfort: $self->{delta_ignore}] //File: $self->{STH_FILE}//", 
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
	  
	### WRITE X-axis VALUES 
	my $divisor = 10 ** (length($max_x) - 1); 
    my $xpd = ceil($max_x/$divisor); #print GLOBAL_LOG "|$xpd|$divisor\n";
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
				       -text => "Load Unit [Mpa/KN]", 
				       -font => $thick_font,
				       -fill => $at,
				      );
    my $Pure_legend = $canvas->createText(
				       120, 80, # X, Y
				       -text    => "EPuRE", 
				       -anchor  => 'e',
				       -font    => 'Courier 20 bold',
				       -fill    => 'pink',
				       -stipple => 'gray50',
				       -tags    => 'item',					   
				      ); 					  
				  
	# X, Y
	my $max_text = sprintf("FT MaX %16s [%8.2f]", "$self->{ftnum_max}","$self->{value_max}");
	my $min_text = sprintf("FT MiN %16s [%8.2f]", "$self->{ftnum_min}","$self->{value_min}");
	$canvas->createText(250, 60, -text => $max_text, -font => $thick_font, -fill => $colour_max,); 
	$canvas->createText(250, 70, -text => $min_text, -font => $thick_font, -fill => $colour_min,);
	
    my $j  = 0; my $x_p  = 'X'; my $m_p = 'M'; my $k = 0;
	my $x_now = 0;
	
	if ($self->{plot_SINGLE_flight} =~m/YES/i)
	  {
	   foreach (@{$self->{UserReqFlightNum_use}})  # USER REQUESTED FLIGHT
         {
			if ($_ =~m/^\s*$/) {next;};
		    my $value = $_ * $self->{overall_factor};
			
			$j++; $k++;
			my $x   =  50 + ($j * 10)  * $scale_x;
			my $m   = $shy - (4 * $value) * $scale_y / 5;
			
			if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m;}

           $canvas->createLine($x_p, $m_p, $x,  $m, -fill => $colour_max,  -width => 0.2);
			$x_p   = $x;
			$m_p   = $m;
			$x_now = $x;
         }
		  $self->{text4user} = sprintf("User Requested Flight No. %-5s","$self->{plot_ONLY_this_FLIGHT}");
		  $canvas->createText(800, 60, -text => $self->{text4user}, -font => $thick_font, -fill => $colour_user,);	 
	  } # end user request
	
	unless ($self->{plot_SINGLE_flight} =~m/YES/i)
	  {
		foreach (@{$self->{MaxFlight_use}})  # MAX. FLIGHT
          {
			if ($_ =~m/^\s*$/) {next;};
		    my $value = $_ * $self->{overall_factor};
			
			$j++; $k++;
			my $x   =  50 + ($j * 10)  * $scale_x;
			my $m   = $shy - (4 * $value) * $scale_y / 5;
			
			if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m;}

            $canvas->createLine($x_p, $m_p, $x,  $m, -fill => $colour_max,  -width => 0.2);
			$x_p   = $x;
			$m_p   = $m;
			$x_now = $x;
          }
		  
        $canvas->createLine($x_now, 400, $x_now,  100, -dash => '-', -fill => 'gold',  -width => 0.2);
        $j = 0; $x_p = 'X'; $m_p = 'M'; $k = 0;
		
		unless ($self->{ftnum_max} =~m/$self->{ftnum_min}/)
		  {
		   foreach (@{$self->{MinFlight_use}})  # MINIMUM FLIGHT
           {
			if ($_ =~m/^\s*$/) {next;};
		    my $value = $_ * $self->{overall_factor};
			
			$j++; $k++;
			my $x   = $x_now + ($j * 10)  * $scale_x;
			my $m   = $shy - (4 * $value) * $scale_y / 5;
			
			if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m;}
			# write code 		
           #$canvas->createLine($x_p, $m_p, $x,  $m, -dash => '-', -fill => $colour_min,  -width => 1);
			$canvas->createLine($x_p, $m_p, $x,  $m, -fill => $colour_min,  -width => 0.2);
			$x_p   = $x;
			$m_p   = $m;
		    #$x_now = $x;
            }
		   $x_now = $x_p;
		  }
        else
          {
		  $self->{text4min} = sprintf("%-30s","$self->{text4min}");
		  $canvas->createText(550, 70, -text => $self->{text4min}, -font => $thick_font, -fill => $colour_min,);
          }	
		  
		unless (($self->{ftnum_long} =~m/$self->{ftnum_min}/) || ($self->{ftnum_long} =~m/$self->{ftnum_max}/))
		{
		   my $text = '[PEAKS]';
		   $canvas->createText($x_now, 80, -text => $text, -font => $thick_font, -fill => $colour_long, -anchor => 'w',); 
           $canvas->createLine($x_now, 400, $x_now,  100, -dash => '-', -fill => 'gold',  -width => 0.2);			   
		   $j = 0; $x_p = 'X'; $m_p = 'M'; $k = 0;
		  foreach (@{$self->{LongFlight_use}})  # LONGEST FLIGHT
            {
			if ($_ =~m/^\s*$/) {next;};
		    my $value = $_ * $self->{overall_factor};
			
			$j++; $k++;
			my $x   = $x_now + ($j * 10)  * $scale_x;
			my $m   = $shy - (4 * $value) * $scale_y / 5;
			
			if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m;}
			# write code 		
           #$canvas->createLine($x_p, $m_p, $x,  $m, -dash => '-', -fill => $colour_min,  -width => 1);
			$canvas->createLine($x_p, $m_p, $x,  $m, -fill => $colour_long,  -width => 0.2);
			$x_p   = $x;
			$m_p   = $m;
		    #$x_now = $x;
            }
		   $x_now = $x_p;
		  }
        else
          {
		  $self->{text4long} = sprintf("%-30s","$self->{text4long}");
		  $canvas->createText(550, 60, -text => $self->{text4long}, -font => $thick_font, -fill => $colour_long,);
          }	  

		if ($self->{do_first_12_ft} =~m/YES/i)  # MULTIPLE FLIGHTS
		  { 
		   my @y = ('80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90','80','90',);
		   for (1 .. $self->{user_selected_ft}) 	   #foreach(sort @{$self->{ftnum_plot_order}})
		     {
			   my $f_num  = $_;
		       my @array  = @{$self->{SelFlight_use}->{$f_num}};
               my $colour = $colours[$f_num];	last if (!defined $colour);	
			   my $yns = 50 + 10*$_;  my $text = '[' . $_ .']';
			   $canvas->createText($x_now, $y[$f_num], -text => $text, -font => $thick_font, -fill => $colour,-anchor => 'w',); 
               $canvas->createLine($x_now, 400, $x_now,  100, -dash => '-', -fill => 'gold',  -width => 0.2);			   
			   $j = 0; $x_p = 'X'; $m_p = 'M'; $k = 0;
			   foreach (@array)
                {
			      if ($_ =~m/^\s*$/) {next;};
		          my $value = $_ * $self->{overall_factor};
			
			      $j++; $k++;
			      my $x   = $x_now + ($j * 10)  * $scale_x;
			      my $m   = $shy - (4 * $value) * $scale_y / 5;
			
			      if ($x_p =~m/X/i) {$x_p = $x; $m_p = $m;}
			      $canvas->createLine($x_p, $m_p, $x,  $m, -fill => $colour,  -width => 0.2);
			      $x_p  = $x;
			      $m_p  = $m;
                }
			   $x_now  = $x_p;
		     }	  
		  } # end Multiple	  
	 } 
    #    my ($l, $r, $t, $b) = $canvas->bbox('all');
    #    $canvas->configure(-scrollregion => ["$l", "$r", "$t", "$b"]);
    #    $canvas->CanvasBind("<Button-1>", sub{$self->printxy($self->{CANVAS},Ev('x'), Ev('y'))});

    $canvas->CanvasBind('<3>' => [sub {area_down(@_)}, \%pinfo]);
    $canvas->CanvasBind('<B3-Motion>' => [sub {area_move(@_)}, \%pinfo]);
	
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
	        $w->addtag('area','withtag',$w->createRectangle($pinfo->{'areaX1'},
		    					    $pinfo->{'areaY1'},$x,$y)
		              );
	        $pinfo->{'areaX2'} = $x;
	        $pinfo->{'areaY2'} = $y;
	      }
      }
   #print GLOBAL_LOG " * scale X: $scale_x \n";  
  } # end of MAiN

  
sub plot_the_min_max_data_files
  {
    my $self = shift;
    print OUTPUT "\@target G0.S1\n";
    print OUTPUT "\@type xy\n";
    my $i = 0; my $valp = 0; my @array_max = (); my @array_min = (); my @array_long = (); my @array_user = ();
    $self->{total_points_accu_x} = 0;

    @{$self->{MaxFlight_use}}    = ();
    @{$self->{MinFlight_use}}    = ();
    @{$self->{LongFlight_use}}   = ();
    @{$self->{UserReqFlightNum_use}} = ();

    if ($self->{plot_SINGLE_flight} =~m/YES/i)   # USER REQUESTED ONLY THIS FLIGHT!
      {
          foreach (@{$self->{UserReqFlightNum}})  
            {
               chomp($_);
               my @block = split(/\s+/,$_); 
               foreach (@block) 
                 {
                   unless ($_ =~m/^\s*$/) 
                     {
                       push (@array_user, $_);
                     }
                 }
            }

          foreach (@array_user)
            {
               if ($_ =~m/^\s*$/) {next;};
               my $value = $_; $i++;
               my $delta = abs($value - $valp);
               unless ($self->{plot_PURE_data} > 0) {if (($delta <= $self->{delta_ignore}) && ($i > 1)) {next;}}
               $valp = $value;   print OUTPUT "$value\n"; 
               push(@{$self->{UserReqFlightNum_use}}, $value);
             }
        $self->{total_points_accu_x}  = $#{$self->{UserReqFlightNum_use}} + 1;
        print OUTPUT "&\n";    
      }  # end USER specific request!
	
    unless ($self->{plot_SINGLE_flight} =~m/YES/i)
	  {
            foreach (@{$self->{MaxFlight}}) # MAXIMUM FLIGHT!
              {
			    chomp($_);
				my @block = split(/\s+/,$_); 
				foreach (@block) 
				  {
				    unless ($_ =~m/^\s*$/) 
					  {
					    push (@array_max, $_);
					  }
				  }
              }

			foreach (@array_max)
              {
				if ($_ =~m/^\s*$/) {next;};
			    my $value = $_; $i++;
				my $delta = abs($value - $valp);
				unless ($self->{plot_PURE_data} > 0) {if (($delta <= $self->{delta_ignore}) && ($i > 1)) {next;}}
				$valp = $value;   print OUTPUT "$value\n"; 
				push(@{$self->{MaxFlight_use}}, $value);
			  }

            print OUTPUT "\@target G0.S2\n";
            print OUTPUT "\@type xy\n";

            foreach (@{$self->{MinFlight}}) # MINIMUM FLIGHT!
              {
			    chomp($_);
			    my @block = split(/\s+/,$_); 
			    foreach (@block) 
				 {
				   unless ($_ =~m/^\s*$/) 
				    {
					  push (@array_min, $_);
					}
				 }
              }

			$valp = 0; 
			$i    = 0;
			foreach (@array_min) 
              {
				if ($_ =~m/^\s*$/) {next;};
			    my $value = $_; $i++;
				my $delta = abs($value - $valp);
                                        unless ($self->{plot_PURE_data} > 0) {if (($delta <= $self->{delta_ignore}) && ($i > 1)) {next;}}
				$valp = $value; print OUTPUT "$value\n";
				push(@{$self->{MinFlight_use}}, $value);
			  }
			  
            print OUTPUT "\@target G0.S3\n";
            print OUTPUT "\@type xy\n";

            foreach (@{$self->{LongFlight}}) # LONGEST FLIGHT!
              {
			    chomp($_);
			    my @block = split(/\s+/,$_); 
			    foreach (@block) 
				 {
				   unless ($_ =~m/^\s*$/) 
				    {
					  push (@array_long, $_);
					}
				 }
              }

			$valp = 0;
			$i    = 0;
			
			foreach (@array_long)   
              {
				if ($_ =~m/^\s*$/) {next;};
			    my $value = $_; $i++;
				my $delta = abs($value - $valp);
				unless ($self->{plot_PURE_data} > 0) {if (($delta <= $self->{delta_ignore}) && ($i > 1)) {next;}}
				$valp = $value; print OUTPUT "$value\n";
				push(@{$self->{LongFlight_use}}, $value);
			  }	  
            print OUTPUT "&\n";
			
			$self->{total_points_accu_x} = $#{$self->{MaxFlight_use}} + $#{$self->{MinFlight_use}} + $#{$self->{LongFlight_use}} + 3;
			
			# Multiple Flights
			if ($self->{do_first_12_ft} =~m/YES/i)
			  {
			   for (1 .. $self->{user_selected_ft})
			     {
				  next unless (defined $self->{SelFlight}->{$_}[0]); #print GLOBAL_LOG "processing: $_\n";
				  my @array = (); my $f_num = $_;
				  foreach (@{$self->{SelFlight}->{$f_num}}) 
                   {
			         chomp($_);
			         my @block = split(/\s+/,$_); 
			         foreach (@block) {unless ($_ =~m/^\s*$/) {push (@array, $_);}}
                   }
				@{$self->{SelFlight}->{$f_num}} = (); # Save Memory! 
				
                $self->{points_this_ft}->{$f_num} = $#array + 1;		
				my $valp = 0;
			    my $j    = 0;
			    foreach (@array)
                 {
				   if ($_ =~m/^\s*$/) {next;};
			       my $value = $_; $j++;
				   my $delta = abs($value - $valp);
				   unless ($self->{plot_PURE_data} > 0) {if (($delta <= $self->{delta_ignore}) && ($i > 1)) {next;}}
				   $valp = $value;
				   push(@{$self->{SelFlight_use}->{$f_num}}, $value);
			     }
                 $self->{total_points_accu_x} = $self->{total_points_accu_x} + $#{$self->{SelFlight_use}->{$f_num}};
				 }
				 
				my @pift = ();
				for (1 .. $self->{user_selected_ft})
				  {
                     my $p = $self->{points_this_ft}->{$_};
					 $self->{zzz}->{$p}  = $_;
				     push(@pift, $p);
				  }
				@pift = sort(@pift); @pift = reverse(@pift);
				foreach(@pift)
				  {
				    my $no = $self->{zzz}->{$_};
				    push(@{$self->{ftnum_plot_order}}, $no); 
					#print GLOBAL_LOG "FTnum: $no |$_|\n";
				  }
			  }
		         @{$self->{MaxFlight}}  = ();
           		 @{$self->{MinFlight}}  = ();
          		 @{$self->{LongFlight}} = ();  
			
			if ($self->{ftnum_max} =~m/$self->{ftnum_min}/) 
			  {
			   $self->{text4min} = 'FT Min   = FT Max';
			   $self->{total_points_accu_x}  = $self->{total_points_accu_x} - $#{$self->{MinFlight_use}};
			  }
			if ($self->{ftnum_min} =~m/$self->{ftnum_long}/) 
			  {
			   $self->{text4long} = 'FT Peaks = FT Min | FT long';
			   $self->{total_points_accu_x} = $self->{total_points_accu_x} - $#{$self->{LongFlight_use}};
			  }
			if ($self->{ftnum_max} =~m/$self->{ftnum_long}/) 
			  {
			   $self->{text4long} = 'FT Peaks = FT Max | FT long';
			   $self->{total_points_accu_x} = $self->{total_points_accu_x} - $#{$self->{LongFlight_use}};
			  }			  
	  }
	if ($self->{total_points_accu_x} < 1) {$self->{error} = 1; print GLOBAL_LOG " *** not enough Peaks to Plot or user Flight requested does not exist!\n";}	  
  }


sub comb_sth_file_for_interesting_flights
  {
    my $self = shift;

    @{$self->{MaxFlight}}  = ();
    @{$self->{MinFlight}}  = ();
    @{$self->{LongFlight}} = ();	
    my @header = ();
    my @names  = ();
    my ($validfor, $block, $name, $points);

    my $stress_factor = $self->{overall_factor};
    my $column        = 8.0;
    my $sum           =   0;
    my ($line, $rows);

    my $i             = 0;
    my $max_stress    = -1e9;
    my $min_stress    = +1e9;
    my ($max_ftype,$min_ftype);
    my ($maxplot_num, $minplot_num);

	$self->{longest_flight_points} = 1; 
	$self->{longest_flight_Number} = 1;
	
    foreach (1 .. 4)
      {
	$line = <INPUT>;
	push(@header, $line);
      }

    for(1 .. 20001) # Max FT number plotted
      {
	last if eof;
	$line     = <INPUT>;
	chop($line);
	$line               =~ s/^\s*//;
	if (($line =~m/^\s*$/) || ($line =~m/^\s*\#/)) {push(@header, $line);next;}
	$i++;
	
	my @a = split(/\s+/, $line);
	if (defined $a[2]) {($validfor, $block, $name) = split(/\s+/, $line);}
	else {($validfor, $block) = split(/\s+/, $line); $name  = 'TF_' . $i; }

	$line     = <INPUT>;
	chop($line);
	$line     =~ s/^\s*//;

	my @b = split(/\s+/, $line);
	if (defined $b[1]) {($points, $name) = split(/\s+/, $line);}
	else {($points) = split(/\s+/, $line);}

	$points =~s/\s*//;
	push(@names, $name);
	if ($points > $self->{longest_flight_points}) {$self->{longest_flight_points} = $points; $self->{longest_flight_Number} = $i; $self->{ftnum_long} = $name;}
	#print GLOBAL_LOG "|$name|";

	$rows     = $points / $column;
	my $int   = floor($rows); # gives the Interger part of Number
	my $dig   = ceil($rows);  # makes 30.25 to become 31
	$rows     = $dig;

    @{$self->{ONEBLOCK}} = ();

	for (1 .. $rows)
	  {
	    $line = <INPUT>;
	    push(@{$self->{ONEBLOCK}}, $line);
	  }

	foreach (@{$self->{ONEBLOCK}})
	  {
	    chomp($_);
	    my @block = split(/\s+/,$_);

	    foreach (@block)
	      {
		unless ($_ =~m/^\s*$/)
		  {	
		    my $value = $stress_factor * $_;

		    if ($value > $max_stress)
		      {
			$max_stress  = $value;
			$max_ftype   = $name;
			$maxplot_num = $i;
		      }

		    if ($value < $min_stress)
		      {
			$min_stress  = $value;
			$min_ftype   = $name;
			$minplot_num = $i;
		      }
		  }
	      }
	  }
	
	if ($maxplot_num == $i) {@{$self->{MaxFlight}}   = @{$self->{ONEBLOCK}};}
	if ($minplot_num == $i) {@{$self->{MinFlight}}   = @{$self->{ONEBLOCK}};}
	if ($self->{longest_flight_Number} == $i) {@{$self->{LongFlight}}   = @{$self->{ONEBLOCK}};}
	
	if ($self->{do_first_12_ft} =~m/YES/i)
	  {
	    if ($i <= $self->{user_selected_ft}) {@{$self->{SelFlight}->{$i}} = @{$self->{ONEBLOCK}};}
	  }
    if ($self->{plot_SINGLE_flight} =~m/YES/i)
      {
        if ($self->{plot_ONLY_this_FLIGHT} == $i) {@{$self->{UserReqFlightNum}}   = @{$self->{ONEBLOCK}};}
      }   
	if (($self->{plot_ONLY_this_FLIGHT} > 0) && ($self->{plot_ONLY_this_FLIGHT} == $i))
	   {
		 foreach(@{$self->{SelFlight}->{$self->{plot_ONLY_this_FLIGHT}}}) 
		   {
		      my @block = split(/\s+/,$_); 
			  foreach (@block) 
				  {
				    unless ($_ =~m/^\s*$/) 
					  {
					    print OUTPUT2 "$_\n";
					  }
				  }
		   }
	   }				  
	$sum = $sum + $validfor * $block;
	#print GLOBAL_LOG sprintf("%4s %10s %s","$i","$name","\n");
      }

	$self->{value_max} = $max_stress;
	$self->{value_min} = $min_stress;
	$self->{ftnum_max} = $max_ftype;	
	$self->{ftnum_min} = $min_ftype;
	
    print OUTPUT "# Plot No. $maxplot_num | Max_Stress: $max_stress | FType: $max_ftype\n";
    print OUTPUT "# Plot No. $minplot_num | Min_Stress: $min_stress | FType: $min_ftype\n";
    print OUTPUT "# TOTAL FLIGHTs:  $sum  \n";
    
    print GLOBAL_LOG "\n   * Summary:\n";
    print GLOBAL_LOG "   * Plot No. $maxplot_num | Max_Stress: $max_stress | FType: $max_ftype\n";
    print GLOBAL_LOG "   * TOTAL FLIGHTs:  $sum  \n";  
  } # STH



sub comb_sigma_file_for_interesting_flights
  {
    my $self = shift;

    @{$self->{MaxFlight}}  = ();
    @{$self->{MinFlight}}  = ();
    @{$self->{LongFlight}} = ();		
    my @header = ();
    my @names  = ();
	my @a      = ();
    my ($f_num, $name, $points);

    my $stress_factor =  $self->{overall_factor};
    my $column        = 10.0;
    my $sum           =    0;
    my ($line, $rows);

    my $i             = 0;
    my $max_stress    = -1e9;
    my $min_stress    = +1e9;
    my ($max_ftype,$min_ftype);
    my ($maxplot_num, $minplot_num);
	
	$self->{longest_flight_points} = 1; 
	$self->{longest_flight_Number} = 1;

    for (1 .. 987654) # Headers 
      {
	$line = <INPUT>;
	push(@header, $line);
	last if ($line =~m/^NBVOL/); # NBVOL (Total Flights)
      }
	  
	my ($a, $total) = split('\s+', $line);  
	
	for(1 .. $total)    # Total Flights in Sigma File
      {
	last if eof;
	# NUVOL - FLIGHT NUMBER
	$line   = <INPUT>;  chop($line);  $line =~ s/^\s*//;
	if (($line =~m/^\s*$/) || ($line =~m/^\s*\#/)) 
	{$line  = <INPUT>;  chop($line);  $line =~ s/^\s*//;}  # in case of 1 empty line after a flight!
	$i++;
	@a      = split(/\s+/, $line);
	if (($a[0] =~m/NUVOL/) && (defined $a[1])) {$f_num = $a[1];}
	else {$f_num = $i;}
	$line   = <INPUT>;  chop($line);  $line =~ s/^\s*//;
    $name   = $line;
    unless ($f_num == $i) {print GLOBAL_LOG " ** Inconsistent Flight-Numbering: Expected Flight [$i] Found [$f_num]\n";}	
	
	# NBVAL - Number of Points in this Flight
    $line   = <INPUT>;  chop($line);  $line =~ s/^\s*//;
	@a      = split(/\s+/, $line);
	if (($a[0] =~m/NBVAL/) && (defined $a[1])) {$points = $a[1];}
	else {print GLOBAL_LOG " *** Points N/A for TF [$i $name] - Rejected Job!\n"; $self->{error} = 1; return;}	
	if ($points > $self->{longest_flight_points}) {$self->{longest_flight_points} = $points; $self->{longest_flight_Number} = $i; $self->{ftnum_long} = $name;}
	push(@names, $name);  #print GLOBAL_LOG "|$name|";
	
    # DATA - for this Flight
	$rows     = $points / $column;
	my $int   = floor($rows); # gives the Interger part of Number
	my $dig   = ceil($rows);  # makes 30.25 to become 31
	$rows     = $dig;

    @{$self->{ONEBLOCK}} = ();

	for (1 .. $rows)
	  {
	    $line = <INPUT>;
	    push(@{$self->{ONEBLOCK}}, $line);
	  }

	foreach (@{$self->{ONEBLOCK}})
	  {
	    chomp($_);
	    my @block = split(/\s+/,$_);

	    foreach (@block)
	      {
		unless ($_ =~m/^\s*$/)
		  {	
		    my $value = $stress_factor * $_;

		    if ($value > $max_stress)
		      {
			$max_stress  = $value;
			$max_ftype   = $name;
			$maxplot_num = $i;
		      }

		    if ($value < $min_stress)
		      {
			$min_stress  = $value;
			$min_ftype   = $name;
			$minplot_num = $i;
		      }
		  }
	      }
	  }
	
	if ($maxplot_num == $i) {@{$self->{MaxFlight}}   = @{$self->{ONEBLOCK}};}
	if ($minplot_num == $i) {@{$self->{MinFlight}}   = @{$self->{ONEBLOCK}};}
	if ($self->{longest_flight_Number} == $i) {@{$self->{LongFlight}}   = @{$self->{ONEBLOCK}};}

	if ($self->{do_first_12_ft} =~m/YES/i)
	  {
	    if ($f_num <= $self->{user_selected_ft}) {@{$self->{SelFlight}->{$f_num}} = @{$self->{ONEBLOCK}};}
	  }
                if ($self->{plot_SINGLE_flight} =~m/YES/i)
                  {
                    if ($self->{plot_ONLY_this_FLIGHT} == $i) {@{$self->{UserReqFlightNum}}   = @{$self->{ONEBLOCK}};}
                  }  
	$sum = $f_num;
	#print GLOBAL_LOG sprintf("%4s %10s %s","$i","$name","\n");
      }

	$self->{value_max} = $max_stress;
	$self->{value_min} = $min_stress;
	$self->{ftnum_max} = $max_ftype;	
	$self->{ftnum_min} = $min_ftype;
	
    print OUTPUT "# Plot No. $maxplot_num | Max_Stress: $max_stress | FType: $max_ftype\n";
    print OUTPUT "# Plot No. $minplot_num | Min_Stress: $min_stress | FType: $min_ftype\n";
    print OUTPUT "# TOTAL FLIGHTs:  $sum  \n";
    
    print GLOBAL_LOG "\n\n\n * Summary:\n";
    print GLOBAL_LOG "# Plot No. $maxplot_num | Max_Stress: $max_stress | FType: $max_ftype\n";
    print GLOBAL_LOG "# TOTAL FLIGHTs:  $sum  \n";
  } # SIGMA


# sub activate_objscan
  # {
    # my $self  = shift;
    # my $mw    = MainWindow->new();
    # $mw->title("Data and Object Scanner");
    # my $scanner   = $mw->ObjScanner(
				    # caller => $self
				   # )->pack(
					   # -expand => 1
					  # );
  # }

# print STDERR "##   This program Plots the Flights with Max & Min Stress from a STH/SIGMA file.\n";
# print STDERR "##    %%%%%Flights with Max & Min Stresses (and Peaks) are searched Automatically!%%%%%\n";
# print STDERR "##        ----->>> Input is the: *.sth / *.sigma file <<<----- \n";
# print STDERR "##        ----->>> .....version with Rforth-Delta-Ignore.......\n";

# # USER INPUT
# $self->{STH_FILE}              = 'MX_HQ.sth';   #  'Point_1_EE.sth';MiX_Mech_EE.sthsfest_pp15.sigma
# $self->{sth_or_sigma}          = 'STH';        # STH or SIGMA
# $self->{overall_factor}        =  1.0;           # Overall Multiplication Factor
# $self->{delta_ignore}          = 1.00;           # rforth value (ignore absolute delta with this value for plots)
# $self->{plot_PURE_data}        =    1;           # 0 or 1 =>>> 1 implies Ignore all RFORT request from user!
# $self->{do_first_12_ft}        = 'YES';           # YES / NO  to plot additional Flights  from 1 to number Stated in "user_selected_ft"
# $self->{user_selected_ft}      =    10;

# $self->{plot_ONLY_this_FLIGHT} =   0;           # Default = 0; Activates the Single Flight Option if > 0 !  Flight Number user wante to plot i.e. 1,2,3 etc. Plots only ONE flight Number stated!

# $self->{do_fixed_scale_x}      = 'NO';
# $self->{force_scale_x_to}      =  0.0018;
# $self->{do_fixed_scale_y}      = 'NO';
# $self->{force_scale_y_to}      =  2.0;
# $self->{shift_Y_text_to}       =  425;           # '100';  # 425 
# $self->{neutral_axis_y_shift}  =  240;        #  Original  290;  
# $self->{plot_parralel_series}  = 'SERIES';  
  
1;
