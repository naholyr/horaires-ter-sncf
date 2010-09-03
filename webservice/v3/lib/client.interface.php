<?php

interface client_interface
{
  
  public function __construct();
  
  public function cherche_gare(&$nom_gare, $id_gare);
  
  public function cherche_departs($nom_gare, $nb_departs);
  
  public function id_gare($nom_gare);
  
  public function cherche_train($num);
  
}