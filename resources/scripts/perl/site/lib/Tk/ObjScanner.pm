package Tk::ObjScanner;

use strict;
use vars qw($VERSION @ISA $errno);

use Carp ;
use Tk::Derived ;
use Tk::Frame;

@ISA = qw(Tk::Derived Tk::Frame);
*isa = \&UNIVERSAL::isa;

$VERSION = sprintf "%d.%03d", q$Revision: 1.1 $ =~ /(\d+)\.(\d+)/;

Tk::Widget->Construct('ObjScanner');

sub Populate
  {
    my ($cw,$args) = @_ ;
    
    require Tk::Menubutton ;
    require Tk::HList ;
    require Tk::ROText ;
    
    $cw->{chief} = delete $args->{'caller'} || delete $args->{'-caller'};

    my $destroyable = defined $args->{'destroyable'} ? 
      delete $args->{'destroyable'} : 1 ;

    croak "Missing caller argument in ObjScanner\n" 
      unless defined  $cw->{chief};

    my $title = delete $args->{title} || delete $args->{-title} ||
      ref($cw->{chief}).' scanner';

    my $menuframe = $cw ->
      Frame (-relief => 'raised', -borderwidth => 2)-> 
        pack(pady => 2,  fill => 'x' ) ;

    my $menu = $cw->{menu}= $menuframe -> Menubutton 
      (-text => $title.' menu') 
          -> pack ( fill => 'x' , side => 'left');

    $menu -> command (-label => 'reload', 
                      command => sub{$cw->updateListBox; });

    my $hlist=  $cw -> Scrolled
      (
       qw\HList -selectmode single -indent 35 -separator |
       -itemtype imagetext \
      )-> pack ( qw/fill both expand 1 /) ;

    $hlist -> configure
      (
       -command => sub 
       {
         my $name = shift ;
         my $item = $hlist->info('data', $name); 
         #print "Double click $name, ref is", ref($item) ,".\n";
         $cw->displaySubItem($name,$item)
       }
      );

    $cw->{itemImg} = $cw->Bitmap(-file => Tk->findINC('file.xbm'));
    $cw->{foldImg} = $cw->Bitmap(-file => Tk->findINC('folder.xbm'));

    $cw->{idx}=0;

    $cw->Advertise(hlist => $hlist);


    my $window = $cw->{dumpWindow} = 
      $cw -> Scrolled('ROText', height => 10)
        -> pack( -fill => 'both') ;

    # add a destroy commend to the menu
    $menu -> command (-label => 'destroy', 
                      command => sub{$cw->destroy; }) if $destroyable ;

    $cw->updateListBox;

    $cw->ConfigSpecs(
                     scrollbars=> [$hlist, undef, undef,'osoe'],
                     width => [$hlist, undef, undef, 80],
                     height => [$hlist, undef, undef, 15],
                     DEFAULT => [$hlist]) ;
    $cw->Delegates(DEFAULT => $hlist ) ;

    $cw->SUPER::Populate($args) ;
  }

sub isObject
  {
    my $ref = shift ; # not a method !
    return 0 unless $ref ;
    return  
      scalar grep ($ref eq $_, qw/REF SCALAR CODE GLOB ARRAY HASH/) ? 0 : 1;
  }

sub updateListBox
  {
    my $cw = shift ;

    my $h = $cw->Subwidget('hlist');
    my $root = 'root';
    #print "root adding $root \n";

    $h->add
      (
       $root,
       -text => "ROOT:".ref($cw->{chief}), 
       -image => $cw->{foldImg},
       -data => $cw->{idx}++ 
      ) unless $h->infoExists($root);

    my @children = $h->infoChildren($root);
    if (scalar @children > 0)
      {
        $cw->{dumpWindow}->delete('1.0','end');
        $h->deleteOffsprings($root);
      }

    $cw->displaySubItem($root,$cw->{chief});
  }

sub displaySubItem
  {
    my $cw = shift ;
    my $name = shift ;
    my $item = shift ;
    my $h = $cw->Subwidget('hlist');

    $cw->{dumpWindow}->delete('1.0','end');

    unless ($name eq 'root')
      {
        my @children = $h->infoChildren($name) ;

        if( scalar @children > 0 ) 
          {
            $h->deleteOffsprings($name);
            return ;
          }
      }

    my $ref = ref($item) ;
    my $isObject = isObject($ref) ;

    #print "ref is $ref\n";
    if (not defined $item)
      {
        #print "adding scalar $name , $item is a scalar\n";
        $cw->{dumpWindow}->delete('1.0','end');
        $cw->{dumpWindow}->insert('end',"undefined value\n");
      }
    elsif (isa($item,'ARRAY'))
      {
        my $i;
        foreach (@$item)
          {
            #print "adding array item $i: $_,",ref($_),"\n";
            my $img = ref($_) ? $cw->{foldImg} : $cw->{itemImg} ;
            $h->addchild($name,
                         -image => $img,
                         -text => '['.$i++."]-> ".$cw->element($_), 
                         -data => $_);
          }
      }
    elsif (isa($item,'REF'))
      {
        $h->addchild($name,
                     -image => $cw->{foldImg},
                     -text => $cw->element($$item), 
                     -data => $$item);
      }
    elsif (isa($item,'SCALAR'))
      {
        $h->addchild($name,
                     -image => $cw->{itemImg},
                     -text => $cw->element($$item), 
                     -data => $$item);
      }
    elsif (isa($item,'CODE') or isa($item,'GLOB'))
      {
        if (isa($item, 'UNIVERSAL'))
          {
            my ($what) = ($item =~ /\b([A-Z]+)\b/);
            $cw->{dumpWindow}-> insert
              ('end', "Sorry, can't display a $what based $ref object");
           }
        else
          {
            $cw->{dumpWindow}->
              insert('end',"Sorry, can't display ".$ref." reference");
          }
      }
    elsif (isa($item, 'HASH'))
      {
        # hash or object

        foreach (sort keys %$item)
          {
            #print "adding hash key $name|$_ ", ref($item->{$_}),"\n";

            my $img = ref($item->{$_}) ? $cw->{foldImg} : $cw->{itemImg} ;
            $h->addchild($name, 
                    -text => "{$_}-> ".$cw->element($item->{$_}),
                    -image => $img,
                    -data => $item->{$_});        
          }
      }
    elsif (defined $item)
      {
        #print "adding scalar $name , $item is a scalar\n";
        $cw->{dumpWindow}->insert('end',$item);
      }
  }

sub element
  {
    my $cw = shift ;
    my $elt = shift;

    my ($what,$nb);
    my $ref = ref($elt) ;

    if (not defined $elt)
      {
        $what =  'undefined';
      }
    elsif ($ref and isa($elt,'UNIVERSAL'))
      {
        my $base ;
        if (isa($elt,'SCALAR')) {$base = 'SCALAR'}
        elsif ($elt =~ /=([A-Z]+)\(/) {$base = $1 ;}
        else {$base = "some magic with $elt" ;}  # desperate measure
        $what = "$ref OBJECT based on $base";
      }
    elsif ($ref)
      {
        # a ref but not an object
        $what = $ref ;
      }
    elsif ($elt =~ /\n/)
      {
        # multi-line string
        $what =  'double click here to display value';
      }
    else
      {
        # plain scalar
        $what =  "'$elt'" ;        
      }
    
    $nb = scalar @$elt if defined $elt && isa($elt,'ARRAY') ;
    $nb = scalar keys(%$elt) if defined $elt && isa($elt,'HASH') ;

    $what .= " ($nb)" if defined $nb;
    return $what ;
  }

1;

__END__

=head1 NAME

Tk::ObjScanner - Tk composite widget object scanner

=head1 SYNOPSIS

  use Tk::ObjScanner;
  
  my $scanner = $mw->ObjScanner( caller => $object, 
                                 [title=>"windows"]) -> pack ;

=head1 DESCRIPTION

The scanner provide a GUI to scan the attributes of an object. It can
also be used to scan the elements of a hash or an array.

The scanner is a composite widget made of a L<Tk::HList> and a text
window (actually a L<TK::ROText>). This widget acts as a scanner to
the object (or hash ref) passed with the 'caller' parameter. The
scanner will retrieve all keys of the hash/object and insert them in
the HList.

When the user double clicks on a key, the corresponding value will be added
in the HList.

If the value is a scalar, the scalar will be displayed in the text window.
(Which is handy if the value is a multi-line string)

=head1 Constructor parameters

=over 4

=item *

caller: The ref of the object or hash or array to scan (mandatory).

=item *

title: the title of the menu created by the scanner (optionnal)

=item *

destroyable: If set, a menu entry will allow the user to deatroy the scanner
widget. (optional, default 1) . You may want to set this parameter to 0 if
the destroy can be managed by a higher level object.

=back

=head1 WIDGET-SPECIFIC METHODS

=head2 updateListBox

Update the keys of the listbox. This method may be handy if the
scanned object wants to update the listbox of the scanner 
when the scanned object gets new attributes.

=head1 CAVEATS

ObjScanner may fail if an object involves a lot of internal perl
magic.  In this case, I'd be glad to hear about and I'll try to fix
the problem.

ObjScanner does not detect recursive data structures. It will just
keep on displaying the tree until the user gets tired of clicking on
the HList items.

=head1 AUTHOR

Dominique Dumont, Dominique_Dumont@grenoble.hp.com

Copyright (c) 1997-1999 Dominique Dumont. All rights reserved.
This program is free software; you can redistribute it and/or
modify it under the same terms as Perl itself.

=head1 SEE ALSO

perl(1), Tk(3), Tk::HList(3)

=cut

