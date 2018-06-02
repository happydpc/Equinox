package Tk::EuLabEntry;

use base  qw(Tk::Frame);
use Tk::widgets qw(Frame Label Entry);
use Data::Dumper;
Construct Tk::Widget 'EuLabEntry';
use strict;
use Tk::ProgressBar;


# my $EuLaEntry = $parent->EuLabEntry(
# 				    -label        => sprintf("%-20s%5s%s",'Labelname','[   ]','*'),
# 				    -labelPack    => [-side => 'left', -anchor => 'w'],
# 				    -width        => 40,
# 				    -textvariable => \$self->{entry_value},
# 				    -nextwidget   => \$next_entry,
# 				    -prevwidget   => \$prev_entry,
#                                   -numberonly   => '1', # 1-> only numbers 0=> any string!
# 				   )->pack(
# 					   -side   => 'top',
# 					   -anchor => 'nw',
# 					   -fill   => 'x',
# 					   -pady   => 1,
# 					   -padx   => 5,
# 					   #-expand => '1'
# 					  );


sub Populate
  {
    require Tk::Entry;

    my($self, $args) = @_;
    $self->SUPER::Populate($args);

    $self->ConfigSpecs
      (
       -nextwidget  => [PASSIVE  => undef, undef, undef ],
       -prevwidget  => [PASSIVE  => undef, undef, undef ],
       -defaulttext => [PASSIVE  => undef, undef, undef ],
      );

    $self->{-textvariable} = delete $args->{-textvariable};
    $self->{-nextwidget}   = delete $args->{-nextwidget};
    $self->{-prevwidget}   = delete $args->{-prevwidget};
    $self->{-defaulttext}  = delete $args->{-defaulltext};
    $self->{-numberonly}   = delete $args->{-numberonly};
    $self->SelectionClear();

    # Advertised subwidgets:  entry.
    my $text = "";
    $text = ${$self->{-textvariable}} if defined $self->{-textvariable};
    $self->{HISTORY} = [$text];
    $text =~ s/\\quote/'/g if defined $text; #'
    ${$self->{-textvariable}} = $text;

    my $frame     = $self->Frame(
				 -relief      => 'groove',
				#-borderwidth => 2,
				)->pack(
					-side   => 'top',
					-anchor => 'w',
					-fill   => 'x',
					-expand => 1, 
					-padx   => 1,
					#-pady   => 1,
				       );

    my $e = $self->{ENTRY} = $frame->Entry(
					   -textvariable => $self->{-textvariable},
					  )->pack(
						  -side   => 'left',
						  -anchor => 'nw',
						  -fill   => 'both',
						  #-pady   => 1,
						  -padx   => 1,
						  -expand => '1'
						 );

    $self->{PERCENT_DONE} = 0;

    $self->{PROGRESS_BAR} = $frame->ProgressBar(
						-background  => 'white',
						-relief => 'flat',
						-length => 10,
						-from   => 0,
						-to     => 10,
						#-blocks => 1,
						-colors => [0, 'red', 10,'red',],
						-variable   => \$self->{PERCENT_DONE},
						-resolution => 0,
					       )->pack(
						       -padx => 5,
						       -pady => 1,
						       -side => 'left',
						      );


    $self->Advertise('entry'   => $e );
    $self->ConfigSpecs(DEFAULT => [$e]);
    $self->Delegates(DEFAULT   => $e);
    $self->AddScrollbars($e) if (exists $args->{-scrollbars});

    $e->bind("<Up>",       sub{$self->gotolast});
    $e->bind("<Down>",     sub{$self->gotonext});
    $e->bind("<Escape>",   sub{$self->escape});
    $e->bind("<Control-z>",sub{$self->escape});
    $e->bind("<FocusIn>",  sub{$self->focusin});
    $e->bind("<FocusOut>", sub{$self->focusout});
   #$e->bind("<Leave>",    sub{$self->focusout});
    $e->bind("<Return>",   sub{$self->gotonext});
    $e->bind('<KeyPress>', [\&save,Tk::Ev('A'), $self]);
    $e->bind("<KP_Enter>", sub{$self->gotonext});

    $self->check_if_entry_is_completed;
  }




sub check_if_entry_is_completed
  {
    my $self  = shift;
    my $entry_text = $self->cget(-textvariable);
    my $text       = $$entry_text;
    my $switch     = '0';

    if (defined   $self->{-numberonly})
      {
	$switch    =  $self->{-numberonly};
      }

    if ((defined $text) && ($text !~m/^\s*$/))
      {
	#print STDERR "text: $text       entry_text:   $entry_text\n";

	$self->{PERCENT_DONE} = 0;
	$self->{PROGRESS_BAR}->update;
      }
    else
      {
	$self->{PERCENT_DONE} = 10;
	$self->{PROGRESS_BAR}->update;
      }

    #print STDERR "switch: $switch\n";

     if (($switch =~m/1/) & (defined $text))
       {
 	$text   =~s/^\s*//;
 	$text   =~s/\s*$//;

 	if ($text =~m/^\d+$|^\d+\.$|^\d+\.\d+$|\.\d+$/)
 	  {
 	    #          12 or 12.  or 12.11   or  .11
 	  }
 	elsif ($text =~m/^e\d+$|^\d+\.e\d+$|^\d+\.\d+e\d+$|\.e\d+$/i)
 	  {
 	    #             e5     or 12.e5      or 12.11e5      or .e5
 	  }
 	elsif ($text =~m/^e\+\d+$|^\d+\.e\+\d+$|^\d+\.\d+e\+\d+$|\.e\+\d+$/i)
 	  {
 	    #             e5     or 12.e5      or 12.11e5      or .e5
 	  }
 	elsif ($text =~m/^e-\d+$|^\d+\.e-\d+$|^\d+\.\d+e-\d+$|\.e-\d+$/i)
 	  {
 	    #             e5     or 12.e5      or 12.11e5      or .e5
 	  }
 	else
 	  {
 	    $self->{PERCENT_DONE} = 10;
 	    $self->{PROGRESS_BAR}->update;
 	  }
       }
  }




sub gotolast
    {
      my $self = shift;
      ${$self->{-prevwidget}}->Subwidget('entry')->tabFocus() if ( 
								  (defined ${$self->{-prevwidget}}) && 
                                                                  (defined ${$self->{-prevwidget}}->Subwidget('entry')) 
                                                                 );
}

sub gotonext
  {
    my $self = shift;
    ${$self->{-nextwidget}}->Subwidget('entry')->tabFocus() if ( 
								(defined ${$self->{-nextwidget}}) && 
                                                                (defined ${$self->{-nextwidget}}->Subwidget('entry')) 
                                                               ); 
  }

sub setnextwidget    
  {
    my $self = shift;
    my $nextw = shift;
    $self->{-nextwidget} =  $nextw if (defined $nextw);
    return $self->{-nextwidget};
  }

sub setprevwidget
  {
    my $self = shift;

    my $prevw = shift;

    $self->{-prevwidget} =  $prevw if (defined $prevw);
    return $self->{-prevwidget};
  }

sub escape
  {
    my $self = shift;

    my $old = 0;
    my $new = 1;
    foreach (my $i = 0;$i <= $#{$self->{HISTORY}};$i++) 
      {
	$new = $self->{HISTORY}->[$i] if defined $self->{HISTORY}->[$i];
	if ($old eq $new) 
	  {
	    splice(@{$self->{HISTORY}},$i,1);
	  }
	$old = $self->{HISTORY}->[$i];
      }

    $self->delete(0,'end');
    my $s = shift(@{$self->{HISTORY}});
    $self->insert('end',$s);
    push(@{$self->{HISTORY}},$s);
  };



sub focusin
  {
    my $self = shift;

    $self->check_if_entry_is_completed;

    my $refentrytext = $self->cget(-textvariable);
    my $defaulttext  = $self->cget(-defaulttext);

    if (defined $refentrytext)
      {
       	my $text = $$refentrytext;
	if (defined $text)
	  {
	    #print STDERR "text: $text\n";
	    if ($text eq "-0-")
	      {
		$self->configure (-defaulttext => '-0-') unless defined $defaulttext;
		$text = "";
		$$refentrytext = $text;
	      }
	  }
	else
	  {
	    $text = "";
	    $$refentrytext = $text;
	  }
      }

    my $gui = $self->toplevel();

    if (defined $gui->Subwidget('Mainmenu')) 
      {
	$gui->Subwidget('Mainmenu')->{UNDO}->configure(-command => sub{$self->escape});
	$gui->Subwidget('Mainmenu')->{COPY}->configure(-command => sub
						       {
							 my $string;
							 $string = $self->SelectionGet() if $self->selectionPresent;
							 return unless defined $string;
							 return if ($string eq '');
							 $self->clipboardClear();
							 $self->clipboardAppend($string);
						       }
						      );
	$gui->Subwidget('Mainmenu')->{CUT}->configure(-command => sub
						       {
							 my $string;
							 $string = $self->SelectionGet() if $self->selectionPresent;
							 return unless defined $string;
							 return if ($string eq '');
							 $self->clipboardAppend($string);
							 if (defined $self->index('sel.first')) 
							   {
							     $self->delete($self->index('sel.first'),$self->index('sel.last'));
							   }
						       }
						      );
	$gui->Subwidget('Mainmenu')->{PASTE}->configure(-command => sub
							{
							  my $string = $self->SelectionGet(-selection => "CLIPBOARD") 
							    if defined 
							      eval
								{ 
								  my $selection = $self->SelectionGet(-selection => "CLIPBOARD")
								};

							  return unless defined $string;
							  return if ($string eq '');
							  $self->insert('insert',$string);
							}
						       );
      }
  }


sub focusout
  {
    my $self = shift;

    $self->check_if_entry_is_completed;

    my $refentrytext = $self->cget(-textvariable);
    my $defaulttext  = $self->cget(-defaulttext);

    if (defined $refentrytext)
      {
	my $text = $$refentrytext;
	if (defined $text) 
	  {
	    if ($text =~/^\s*$/)
	      {
		$text = $defaulttext if defined $defaulttext;
	      }

	    $$refentrytext = $text;
	  }
      }
  }

sub settextvariable
  {
    my $self = shift;
    my $textvariable = shift;

    $$textvariable = "-0-" unless defined $$textvariable;
    $$textvariable = "" if $$textvariable eq "";

    $self->{-textvariable} = $textvariable;
    $self->configure(-textvariable => $textvariable);

    $self->focusin;
    $self->focusout;
  }


sub save
  {
    my $w = shift;
    my $s = shift;
    my $self = shift;

    $self->check_if_entry_is_completed; 

    splice(@{$w->parent->{HISTORY}},0,0,$w->get());
  }



sub tabFocus 
  {
    my $w  = shift;
    my $e = $w->Subwidget('entry');

    $e->tabFocus() if defined $e; 
  }




1;
