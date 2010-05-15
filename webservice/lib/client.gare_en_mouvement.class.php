<?php

require_once dirname(__FILE__) . '/client.interface.php';

class client_gare_en_mouvement implements client_interface
{

  private static $ids = array(
    'lyon-part-dieu' => 'LYD',
    'paris-gare-de-lyon' => 'PLY',
    'lille-europe' => 'LEW',
  );

  public function __construct()
  {
  }

  public function cherche_gare(&$nom_gare, $id_gare)
  {
    $real_id = $this->id_gare($nom_gare);
    if (is_null($real_id)) {
      erreur('Gare "' .$nom_gare. '" inconnue pour Gares En Mouvement', 330);
    }
    if (!is_null($id_gare) && $real_id != $id_gare) {
      erreur('ID "' .$id_gare. '" invalide pour la gare "' .$nom_gare. '" (ID réel "' .$real_id. '"', 331);
    }
    return array(utf8_encode(nom_gare($nom_gare)) => $real_id);
  }

  public function cherche_departs($nom_gare, $nb_departs)
  {
    $departs = array();
    $id = $this->id_gare($nom_gare);
    $url = 'http://www.gares-en-mouvement.com/include/tvs.php?nom_gare=&tab=dep&TVS=http://www.gares-en-mouvement.com/tvs/TVS?wsdl&tab_summary_dep=&caption=&type=T&numero=N&num=N&heure=H&dest=D&origine=P&situation=S&voie=V&color=bleu&heur=h&h=h&minut=min&m=min&arriv=A&retard=RETARD&code_tvs=' . rawurlencode($id);
    $html = file_get_contents($url);
    if (preg_match('#<tbody>(.*?)</tbody>#si', $html, $tbody)) {
      $tbody = $tbody[1];
      preg_match_all('#<tr.*?>(.*?)</tr>#si', $tbody, $rows, PREG_SET_ORDER);
      foreach ($rows as $row) {
        $row = $row[1];
        preg_match_all('#<td.*?>(.*?)</td>#si', $row, $cells, PREG_SET_ORDER);
        $type = trim($cells[0][1]);
        $num = trim($cells[1][1]);
        if ($num == '' || $num == '&nbsp;') {
          continue;
        }
        $heure = trim(strip_tags($cells[2][1]));
        $dest = trim($cells[3][1]);
        $situation = trim(strip_tags($cells[4][1]));
        $voie = trim(strip_tags($cells[5][1]));
        $depart = array(
          'attention' => '',
          'type' => preg_match('#alt="(.*?)"#i', $type, $m) ? $m[1] : strip_tags($type),
          'numero' => $num,
          'heure' => $heure,
          'destination' => $dest,
          'voie' => $voie,
          'supprime' => preg_match('#SUPPR#i', $situation) ? true : false,
          'retards' => array(),
          'source' => 'gare-en-mouvement',
        );
        if (preg_match('#RETARD *: *(.*?)$#i', $situation, $m)) {
          $depart['retards'][] = array('retard' => $m[1], 'motif' => '');
        }
        $departs[] = $depart;
      }
    }
    return $departs;
  }

  public function cherche_train($num)
  {
    return null;
  }

  public function id_gare($nom_gare)
  {
    $key = str_replace(' ', '-', strtolower(iconv("UTF-8", "ASCII//TRANSLIT", $nom_gare)));
    if (isset(self::$ids[$key])) {
      return self::$ids[$key];
    } else {
      return null;
    }
  }

}