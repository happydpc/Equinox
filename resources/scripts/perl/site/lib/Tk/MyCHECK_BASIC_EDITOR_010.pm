package Tk::MyCHECK_BASIC_EDITOR_010;

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
	$self->{tool_version}  = '1.0';
	
    if (@_)
      {
        my %extra = @_;
        @$self{keys %extra} = values %extra;
      }

    my $root                = $self->{ROOT};  
	@{$self->{log_files}}   = @{$self->{ROOT}->{log_files}};
	
	$self->activate_text_editor($root);
  }

  
  

sub activate_text_editor
  {
    my $self  = shift;  
    my $root  = shift;    
	
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

	my @array = ();												  
    foreach (@{$self->{log_files}})
      {
	    my $file = $_;
        open(REPORT,  "<". $file);
        my @a  = <REPORT>;
        close(REPORT);
		push(@array, @a)
      }
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


  
  
1;