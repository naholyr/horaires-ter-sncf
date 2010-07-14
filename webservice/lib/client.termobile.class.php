<?php

require_once dirname(__FILE__) . '/client.interface.php';

if (!function_exists('curl_exec')) {
  dl('php_curl.dll');
}

class client_termobile implements client_interface
{

  private $s;
  private $cookie;
  private $ids = array();

  public function __construct()
  {
    $this->init_curl_cookie('http://www.termobile.fr/pages/mobi/accueil.jsp');
  }

  // Initialiser la session cURL
  private function init_curl_cookie($url)
  {
    $this->s = curl_init();
    curl_setopt($this->s, CURLOPT_HTTPHEADER, array(
    'Keep-Alive: 300',
    'Connection: keep-alive',
    'Cache-Control: max-age=0',
    ));
    curl_setopt($this->s, CURLOPT_TIMEOUT, 10);
    curl_setopt($this->s, CURLOPT_MAXREDIRS, 4);
    curl_setopt($this->s, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($this->s, CURLOPT_USERAGENT, 'Mozilla/5.0 (X11; U; Linux i686; fr; rv:1.9.1.8) Gecko/20100214 Ubuntu/9.10 (karmic) Firefox/3.5.8 GTB6');

    // Cookie de session
    curl_setopt($this->s, CURLOPT_HEADER, true);
    curl_setopt($this->s, CURLOPT_NO_BODY, true);
    curl_setopt($this->s, CURLOPT_URL, $url);
    $header_string = curl_exec($this->s);
    if (!$header_string) {
      erreur('Erreur serveur termobile.fr inaccessible ! Réessayez plus tard', 400);
    }
    if (preg_match('#Set-Cookie *: *(.*?)(?:\n|$)#i', $header_string, $m)) {
      $this->cookie = trim($m[1]);
    } else {
      erreur('Erreur serveur (cookie de session)', 320);
    }
  }

  // Post data and retrieve html/status
  private function do_post($url, $post, &$status)
  {
    curl_setopt($this->s, CURLOPT_COOKIE, $this->cookie);
    curl_setopt($this->s, CURLOPT_URL, $url);
    curl_setopt($this->s, CURLOPT_POST, true);
    curl_setopt($this->s, CURLOPT_HEADER, false);
    curl_setopt($this->s, CURLOPT_NO_BODY, false);
    curl_setopt($this->s, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($this->s, CURLOPT_POSTFIELDS, $post);

    $status = curl_getinfo($this->s, CURLINFO_HTTP_CODE);
    if (substr($status, 0, 1) != '2' && $status != 302) { // 2xx = "OK", 302 = "found"
      return false;
    }

    return curl_exec($this->s);
  }

  public function cherche_gare(&$nom_gare, $id_gare)
  {
    // Rechercher la gare
    $html = $this->do_post('http://www.termobile.fr/confirmerGareRPD.do', array('valeurGare' => $nom_gare), $status);
    if ($html === false) {
      erreur("Erreur serveur (status = $status)", 321);
    }

    // Extraire la liste des gares correspondant au nom demandé
    $gares = array();
    if (preg_match('#<select.*? name=[\'"]?idxGare[\'"]?.*?>(.*?)</select>#is', $html, $m1)) {
      $options = trim($m1[1]);
      preg_match_all('#<option.*?value=[\'"]?(\d+)[\'"]?.*?>(.*?)</option>#i', $options, $m2,  PREG_SET_ORDER);
      foreach ($m2 as $g) {
        $gares[utf8_encode(nom_gare($g[2]))] = intval($g[1]);
      }
    }
    elseif (preg_match('#Aucune gare#i', $html)) {
      erreur('Aucune gare trouvee', 330);
    } else {
      erreur('Erreur inconnue', 340, $html);
    }

    // Recherche de l'ID gare selon les résultats
    if (count($gares) == 0) {
      erreur('Aucune gare trouvee (bis)', 331);
    } elseif (count($gares) == 1) {
      $id_gare = reset($gares);
    } else {
      // Chercher avec le nom directement
      $nom_gare_sans_accent = strtolower(iconv("UTF-8", "ASCII//TRANSLIT", $nom_gare));
      foreach ($gares as $nom => $id) {
        $nom_sans_accent = strtolower(iconv("UTF-8", "ASCII//TRANSLIT", $nom));
        if ($nom_sans_accent == $nom_gare_sans_accent) {
          $id_gare = $id;
          break;
        }
      }
    }

    // Résolution du nom de la gare
    if (DEBUG) var_dump("avant=".$nom_gare);
    if (is_null($id_gare)) {
      return $gares;
    } elseif (!in_array($id_gare, $gares)) {
      erreur('ID invalide', 350, array('gares' => $gares, 'nom' => $nom_gare, 'id' => $id_gare));
    } else {
      $nom_gare = array_search($id_gare, $gares, true);
      if (DEBUG) var_dump("apres=".$nom_gare);
      if ($nom_gare === false) {
        erreur('ID invalide (bis)', 351, array('gares' => $gares, 'nom' => $nom_gare, 'id' => $id_gare));
      } else {
        if (DEBUG) $this->fixNomGare($nom_gare);
        if (DEBUG) var_dump("fix=".$nom_gare);
        $this->ids[$nom_gare] = $id_gare;
        return array($nom_gare => $id_gare);
      }
    }
  }

  // FIX 14/07/2010 : termobile.fr ajoute maintenant le nom de la commune � la fin, format = gare - commune
  protected function fixNomGare(&$nom_gare)
  {
    if (preg_match('#^ *(.+?) - .{1,24}$#', $nom_gare, $match)) {
      $nom_gare = $match[1];
    }
  }

  public function cherche_departs($nom_gare, $nb_departs)
  {
    $id_gare = $this->id_gare($nom_gare);

    $html = $this->do_post('http://www.termobile.fr/rechercherRPD.do', array('idxGare' => $id_gare, 'idxNombre' => $nb_departs), $status);
    if ($html === false) {
      erreur("Erreur serveur (status = $status)", 322);
    }

    // Analyser le résultat
    $departs = array();
    $html = str_replace(array("\r", "\n", '&nbsp;', "\t"), array('', '', ' ', ' '), $html);
    preg_match_all('#<td.*? class="train"> *<table.*?> *<tr> *(.*?</tr> *<tr>.*?<table.*? class="sticker">.*?</table>.*?) *</tr> *</table> *</td>#i', $html, $m, PREG_SET_ORDER);
    foreach ($m as $item) {
      $item = utf8_encode(trim($item[1]));
      $depart = array();
      if (preg_match('#<td.*? class=[\'"]trainInfos[\'"].*?> *<span.*? class=[\'"]purpleL[\'"].*?> *(.*?) *</span#i', $item, $m2)) {
        $depart['type'] = trim($m2[1], ' -');
      }
      if (preg_match('#<td.*? class=[\'"]schedule[\'"].*?> *<span.*? class=[\'"]black[\'"].*?> *N° *</span> *(.*?) *<br#i', $item, $m2)) {
        $depart['numero'] = trim($m2[1]);
      }
      if (preg_match('#<td.*? class=[\'"]schedule[\'"].*?> *<span.*? class=[\'"]blackB[\'"].*?> *(\d+h\d+) *</span#i', $item, $m2)) {
        $depart['heure'] = trim($m2[1]);
      }
      if (preg_match('#Destination.*?<span.*? class=[\'"]blackB[\'"].*?>(.*?)</span#i', $item, $m2)) {
        $depart['destination'] = nom_gare(trim($m2[1]));
      }
      if (preg_match('#Supprim#i', $item)) {
        $depart['supprime'] = true;
      }
      if (preg_match('#Départ dans (.*?)<#i', $item, $m2)) {
        $depart['attention'] = trim($m2[1]);
      }
      preg_match_all('#<p.*? class="alert".*?> *(.*?) *</p#i', $item, $m2, PREG_SET_ORDER);
      $depart['retards'] = array();
      foreach ($m2 as $item2) {
        $item2 = $item2[1];
        $retard = array();
        if (preg_match('#Retard[^<]*?</span> *(.*?) *<br#i', $item2, $m3)) {
          $retard['retard'] = trim($m3[1]);
        } else {
          continue;
        }
        if (preg_match('#Motif[^<]*?</span> *(.*?) *<br#i', $item2, $m3)) {
          $retard['motif'] = trim($m3[1]);
        }
        $depart['retards'][] = $retard;
      }
      $depart['source'] = 'termobile';
      $departs[] = $depart;
    }
    return $departs;
  }

  public function cherche_train($num)
  {
    // Rechercher le train
    $html = $this->do_post('http://www.termobile.fr/rechercherRMT.do', array('numTrain' => $num, 'dateTrain' => date('Y|m|d')), $status);
    if ($html === false) {
      erreur("Erreur serveur (status = $status)", 321);
    }
    // Extraire la liste des arrêts du train demandé
    $info = array();
    $html = str_replace(array("\r", "\n", '&nbsp;', "\t"), array('', '', ' ', ' '), $html);
    if (preg_match('#<table.*? id=[\'"]content[\'"].*?> *(.+) *</table> *</form>#i', $html, $match)) {
      $body = utf8_encode($match[1]);
      // Numéro
      if (preg_match('#N° *: *<span.*? class=[\'"]blackB[\'"].*?> *(.*?) *<#i', $body, $m)) {
        $info['numero'] = trim($m[1]);
      }
      // Date
      if (preg_match('#<p.*? class=[\'"]blueL[\'"].*?>le (.*?) *</p#i', $body, $m)) {
        $info['date'] = trim($m[1]);
      }
      // Départ/Arrivée
      $info['heures'] = array();
      if (preg_match('#D(?:é|&eacute;)part *: *<br.*?> *<span.*? class=[\'"]blackB[\'"].*?> *(.*?) *</span#i', $body, $m)) {
        $gare_depart = nom_gare($m[1]);
        if (preg_match('#<table.*? class=[\'"]sticker[\'"].*?> *<td.*? class=[\'"]trainPic[\'"].*?>.*?<td.*?><span.*? class=[\'"]blackB[\'"].*?> *(.*?) *</span#i', $body, $m)) {
          $info['heures'][$gare_depart] = $m[1];
        }
      }
      if (preg_match('#Destination *: *<br.*?> *<span.*? class=[\'"]blackB[\'"].*?> *(.*?) *</span#i', $body, $m)) {
        $gare_arrivee = nom_gare($m[1]);
        if (preg_match('#<tr.*? class=[\'"]arrival[\'"].*.>.*?Arriv(?:é|&eacute;)e *: *<span.*? class=[\'"]blackB[\'"].*?> *(.*?) *</span.*?</tr#i', $body, $m)) {
          $info['heures'][$gare_arrivee] = $m[1];
        }
      }
      // Arrêts
      $info['arrets'] = array();
      if (preg_match('#Arr(?:ê|&ecirc;)ts desservis.*?<ul>(.*?)</ul>#i', $body, $m)) {
        preg_match_all('#<li> *(.*?) *</li#i', $m[1], $m, PREG_SET_ORDER);
        foreach ($m as $arret) {
          $info['arrets'][] = trim(nom_gare($arret[1]));
        }
      }
    } else {
      erreur("Mauvais format de la réponse", 330);
    }
    return $info;
  }

  public function id_gare($nom_gare)
  {
    return $this->ids[$nom_gare];
  }

}