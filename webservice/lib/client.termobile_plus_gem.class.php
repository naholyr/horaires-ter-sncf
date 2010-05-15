<?php

require_once dirname(__FILE__) . '/client.interface.php';

class client_termobile_plus_gem implements client_interface
{

  private $client_termobile;
  private $client_gem;

  private $sources = array();
  
  public function __construct()
  {
    $this->client_termobile = get_client('termobile');
    $this->client_gem = get_client('gare_en_mouvement');
  }

  public function cherche_gare(&$nom_gare, $id_gare)
  {
    $sources = array();
    try {
      $result = $this->client_termobile->cherche_gare($nom_gare, $id_gare);
      $sources[] = 'termobile';
      try {
        $this->client_gem->cherche_gare($nom_gare, null);
        $sources[] = 'gem';
      } catch (ClientError $e) {
        // skip
      }
    } catch (ClientError $e1) {
      try {
        $result = $this->client_gem->cherche_gare($nom_gare, $id_gare);
        $sources[] = 'gem';
      } catch (ClientError $e2) {
        throw $e1;
      }
    }
    $this->sources[$nom_gare] = $sources;
    return $result;
  }

  public function cherche_departs($nom_gare, $nb_departs)
  {
    if (in_array('termobile', $this->sources[$nom_gare])) {
      $departs = $this->client_termobile->cherche_departs($nom_gare, $nb_departs);
    } else {
      $departs = array();
    }
    if (in_array('gem', $this->sources[$nom_gare])) {
      $departs_gem = $this->client_gem->cherche_departs($nom_gare, $nb_departs);
    } else {
      $departs_gem = array();
    }
    $added = false;
    foreach ($departs_gem as $depart_gem) {
      $found = false;
      foreach ($departs as &$depart) {
        if ($this->meme_depart($depart, $depart_gem)) {
          $depart['voie'] = $depart_gem['voie'];
          $found = true;
          break;
        }
      }
      if (!$found) {
        $departs[] = $depart_gem;
        $added = true;
      }
    }
    if ($added) {
      usort($departs, array($this, 'cmp_departs'));
      $departs = array_slice($departs, 0, $nb_departs);
    }
    return $departs;
  }
  
  private function cmp_departs($d1, $d2) 
  {
    $h1 = $d1['heure'];
    $h2 = $d2['heure'];
    return strcmp($h1, $h2);
  }
  
  private function meme_depart($d1, $d2)
  {
    // Même train
    if ($d1['numero'] == $d2['numero']) {
      return true;
    }
    // Cas particulier : entre GEM et TERMobile on a parfois pour les TER un décalage d'1 pour les numéro
    if (is_numeric($d1['numero']) && is_numeric($d2['numero']) && $d1['heure'] == $d2['heure']) {
      return true;
    }
    return false;
  }

  public function id_gare($nom_gare)
  {
    if (isset($this->sources[$nom_gare])) {
      if (in_array('termobile', $this->sources[$nom_gare])) {
        return $this->client_termobile->id_gare($nom_gare);
      } elseif (in_array('gem', $this->sources[$nom_gare])) {
        return $this->client_gem->id_gare($nom_gare);
      }
    }
    return null;
  }

  public function cherche_train($num)
  {
    return $this->client_termobile->cherche_train($num);
  }
  
}