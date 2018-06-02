#! /opt/perl5/bin/perl -w

use strict;
use Tk; 

my $root                         = {};
$root->{global_inputfilename}    = 'HQ_GLOBAL_INPUT_FILE.hqx';

if (defined $ARGV[0])  {$root->{global_inputfilename}    = $$ARGV[0]; print STDERR "using inputfile: $ARGV[0]\n";}
&load_hqx_global_input_information($root);
&activate_text_editor($root);

MainLoop;



sub activate_text_editor
  {
    my $root  = shift;    
    my $file  = $root->{GLOBAL}->{GLOBAL_LOGFILE};
	
	$root->{G_TOP} =  MainWindow->new();
	$root->{G_TOP}->optionAdd('*font' => 'Courier 10');
    $root->{G_TOP}->geometry("800x400+100+100");
    $root->{G_TOP}->title("MyCHECK Quick Viewer v1.000!");	   

    $root->{LOGTEXT} = $root->{G_TOP}->Scrolled(
				                                "Text",
				                                -width      => 50,
				                                -height     => 30,	
				                                -scrollbars => 'se',
				                                -cursor     => 'pirate',
				                                -wrap       => "none",
				                                -font       => 'Courier 8 bold',
			                                   )->pack(
				                                        -side => 'top',
				                                        -fill => 'both',
				                                        -expand => 1,
				                                      );

    open(REPORT,  "<". $file);
    my @array  = <REPORT>;
    close(REPORT);

    $root->{FILE_CONTENTS} = \@array;
    &reformat_all($root);
  }


sub reformat_all
  {
    my $root  = shift;
    my @array = @{$root->{FILE_CONTENTS}};

    $root->{LOGTEXT}->tagConfigure('red',    -foreground =>'red',    -underline => 1);
    $root->{LOGTEXT}->tagConfigure('red2',   -foreground =>'red',   );
    $root->{LOGTEXT}->tagConfigure('blue',   -foreground =>'blue',  );
    $root->{LOGTEXT}->tagConfigure('green',  -foreground =>'green', );
    $root->{LOGTEXT}->tagConfigure('orange', -foreground =>'orange', );
    $root->{LOGTEXT}->tagConfigure('grey',   -foreground =>'grey', );  
	$root->{LOGTEXT}->tagConfigure('brown',  -foreground =>'brown', );
	$root->{LOGTEXT}->tagConfigure('Gold',   -foreground =>'Gold', );
    $root->{LOGTEXT}->tagConfigure('purple', -foreground =>'purple',);

    my $i     = 0;
    my $color = 'purple';

    foreach (@array) 
      {
	$i++;
	my $line = $_;

	if (($line =~m/<<START>>/i) || ($line =~m/<<END>>/i))            {$root->{LOGTEXT}->insert("end", $line, 'purple');}
	elsif (($line =~m/^\s*\*\*\*/i) || ($line =~m/ERROR|WARN/i))     {$root->{LOGTEXT}->insert("end", $line, 'red2');}
	elsif (($line =~m/^\s*\|/) || ($line =~m/^\s*\*\s*\|/))          {$root->{LOGTEXT}->insert("end", $line, 'green');}
	elsif ($line =~m/\=/i)                                           {$root->{LOGTEXT}->insert("end", $line, 'brown'); }
	elsif (($line =~m/^\s*\*\s*Max/i) || ($line =~m/^\s*\*\s*Min/i)) {$root->{LOGTEXT}->insert("end", $line, 'purple'); }
	elsif ($line =~m/^\s*\*/i)                                       {$root->{LOGTEXT}->insert("end", $line, 'grey'); }	
	elsif ($line =~m/^\s*</)                                         {$root->{LOGTEXT}->insert("end", $line, 'Gold');}
	else                                                             {$root->{LOGTEXT}->insert("end", $line);}	
      }
  }



sub load_hqx_global_input_information
  {
    my $root = shift;
	
	open(INPUT,  "<". $root->{global_inputfilename}) or print STDERR " *** Inputfile not Found |$root->{global_inputfilename}|\n";
    my @input = <INPUT>;
	chop(@input);
	close(INPUT);
	
	my $type                    = 'NA';    
	$root->{number_of_missions} = 0;
	foreach (@input)
	  {
		if (($_ =~m/^\s*$/) || ($_ =~m/^\s*\#/)) {next;}
    
		if ($_ =~m/^\s*BEGIN_GLOBAL/i)               {$type = 'GLOBAL';        next;} #
		if ($_ =~m/^\s*END/i)                        {$type = 'NA';            next;} #
		if ($_ =~m/^\s*BEGIN_MISSION/i)              {$type = 'MISSION'; $root->{number_of_missions}++; next;} #
		if ($_ =~m/^\s*BEGIN_OPTIONS_COUNT_ANA/i)    {$type = 'COUNT_ANA';     next;} #	
		if ($_ =~m/^\s*BEGIN_OPTIONS_GENERATE_STH/i) {$type = 'GENERATE_STH';  next;} #	
		if ($_ =~m/^\s*BEGIN_OPTIONS_PLOTS/i)        {$type = 'PLOT_OPTIONS';  next;} #	
		if ($_ =~m/^\s*BEGIN_THIS_AC_4_DIGIT_ROT_MLG_CODES/i) {$type = 'ROT_MLG_CODES';  next;}	
		
    	if ($type eq 'GLOBAL') 
	       {
    	      my ($key, $value) = split('\s*=\s*', $_);
		      if((defined $key) && (defined $value))
		         {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
					$root->{GLOBAL}->{$key}  = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
		         }		  
	       }	   
    	if ($type eq 'MISSION') 
	       {
    	      my ($key, $value) = split('\s*=\s*', $_);
		      if((defined $key) && (defined $value))
		         {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) 
					    {
						   $value =~s/^\s*//; 
						   my @a = split('#', $value); 
						   $value = $a[0]; $value =~s/^\s*//; $value =~s/\s*$//;
						} 
					else 
					    {
						   $value =~s/^\s*//; $value =~s/\s*$//;
						} 
					$root->{MISSION}->{$root->{number_of_missions}}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
		         }		  
	       }
    	if ($type eq 'COUNT_ANA') 
	       {
    	      my ($key, $value) = split('\s*=\s*', $_);
		      if((defined $key) && (defined $value))
		         {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
					$root->{COUNT_ANA}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
		         }		  
	       }
    	if ($type eq 'GENERATE_STH') 
	       {
    	      my ($key, $value) = split('\s*=\s*', $_);
		      if((defined $key) && (defined $value))
		         {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
					$root->{GENERATE_STH}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
		         }		  
	       }
    	if ($type eq 'PLOT_OPTIONS') 
           {
	          my ($key, $value) = split('\s*=\s*', $_);
	          if((defined $key) && (defined $value))
	             {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
		            $root->{PLOT_OPTIONS}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
	             }		  
           }
    	if ($type eq 'ROT_MLG_CODES') 
           {
	          my ($key, $value) = split('\s*=\s*', $_);
	          if((defined $key) && (defined $value))
	             {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0];} else {$value =~s/^\s*//;} 
		            $root->{ROT_MLG_CODES}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
	             }		  
           }
      } # end loop
  } # end sub


