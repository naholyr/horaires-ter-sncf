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
    // On complète les informations de voie avec les départs de gare-en-mouvement
    // On ajoute tous les départs gare-en-mouvement qui ne sont pas inclus
    foreach ($departs_gem as $depart_gem) {
      $found = false;
      foreach ($departs as &$depart) {
        if ($this->meme_depart($depart, $depart_gem)) {
          $depart['voie'] = $depart_gem['voie'];
          if (count($depart['retards']) == 0 && count($depart_gem['retards']) != 0) {
            $depart['retards'] = $depart_gem['retards'];
          }
          $found = true;
          break;
        }
      }
      // Désactivation de l'ajout des horaires de gare-en-mouvement
      // Le temps de résoudre le problème de détection des doublons correcte
      if (!$found) {
        $departs[] = $depart_gem;
        $added = true;
      }
    }
    // Supprimer les doublons
    // Désactivation de la suppression des doublons : les doublons termobile sont normaux et
    // affichés en gare, donc on les garde. Reste le problème des vrais doublons causés par
    // ajout des horaires GEM.
    if ($added) {
      $departs_uniq = array();
      foreach ($departs as $depart) {
        foreach ($departs_uniq as &$depart_uniq) {
          if ($depart['source'] != $depart_uniq['source'] && $this->meme_depart($depart, $depart_uniq)) {
            if (isset($depart['voie']) && $depart['voie'] != '' && (!isset($depart_uniq['voie']) || $depart_uniq['voie'] == '')) {
              $depart_uniq['voie'] == $depart['voie'];
            }
            if (isset($depart['retards']) && count($depart['retards']) == 0 && (!isset($depart_uniq['retards']) || count($depart_uniq['retards']) == 0)) {
              $depart_uniq['retards'] = $depart['retards'];
            }
            continue 2;
          }
        }
        $departs_uniq[] = $depart;
      }
      $departs = $departs_uniq;
    }
    // Trier s'il y a eu des ajouts
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
    // Cas particulier : entre GEM et TERMobile on n'a parfois pas les mêmes numéros pour un même train
    if ($d1['heure'] == $d2['heure']) {
      // On cherche les mêmes heures de départ + même destination
      // Encore une différence : les destination ne sont pas indiquées de la même manière, on vérifie simplement que
      // l'une est contenue dans l'autre
      $k1 = str_replace(' ', '-', strtolower(iconv("UTF-8", "ASCII//TRANSLIT", $d1['destination'])));
      $k2 = str_replace(' ', '-', strtolower(iconv("UTF-8", "ASCII//TRANSLIT", $d2['destination'])));
      $v1 = isset($d1['voie']) ? $d1['voie'] : '';
      $v2 = isset($d2['voie']) ? $d2['voie'] : '';
      if ((strpos($k1, $k2) !== false || strpos($k2, $k1) !== false) && ($v1 == '' || $v2 == '' || $v1 == $v2)) {
        return true;
      }
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