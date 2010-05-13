<?php

if (isset($_GET['source']) && $_GET['source'] == 1) { highlight_file(__FILE__); exit(0); }

// Configuration
$nb_max_departs = 20;
$nb_min_departs = 4;
$cache_timeout = 300;
$cache_enabled = true;

// Compat
if (!function_exists('json_encode')) {
  require dirname(__FILE__) . '/JSON.php';
  function json_encode($data) {
    static $json = null;
    if (is_null($json)) {
      $json = new Services_JSON();
    }
    return $json->encode($data);
  }
}

// Nettoyage des noms de gares
function nom_gare($nom)
{
  return preg_replace('/^gare d[e\'] */i', '', $nom);
}

// Retour erreur
function erreur($message, $code, $additional_info = null)
{
  header('Content-type: application/json');
  echo json_encode(array('code' => $code, 'error' => $message, 'info' => $additional_info));
  exit(0);
}

// Retour succès
function retour($data, $code = 200)
{
  header('Content-type: application/json');
  echo $contents = json_encode(array('code' => $code, 'success' => $data));
  if (isset($GLOBALS['cache.key'])) {
    do_cache($GLOBALS['cache.key'], $contents);
  }
  exit(0);
}

// Test de cache et renvoi si toujours en cache
function cache($key, $cache_timeout)
{
  $file = 'cache/'.$key;
  if (file_exists($file)) {
    if (time() - filemtime($file) < $cache_timeout) { // cached ?
      header("HTTP/1.0 200 CACHE OK");
      header('Content-type: application/json');
      echo file_get_contents($file);
      exit(0);
    } else {
      unlink($file);
    }
  }
  $GLOBALS['cache.key'] = $key;
}

// Mise en cache
function do_cache($key, $content)
{
  $file = 'cache/'.$key;
  file_put_contents($file, $content);
}

// Initialiser la session cURL
function init_curl_cookie($url, &$cookie)
{
  $s = curl_init();
  curl_setopt($s, CURLOPT_HTTPHEADER, array(
    'Keep-Alive: 300',
    'Connection: keep-alive',
    'Cache-Control: max-age=0',
  ));
  curl_setopt($s, CURLOPT_TIMEOUT, 10);
  curl_setopt($s, CURLOPT_MAXREDIRS, 4);
  curl_setopt($s, CURLOPT_RETURNTRANSFER, true);
  curl_setopt($s, CURLOPT_USERAGENT, 'Mozilla/5.0 (X11; U; Linux i686; fr; rv:1.9.1.8) Gecko/20100214 Ubuntu/9.10 (karmic) Firefox/3.5.8 GTB6');

  // Cookie de session
  curl_setopt($s, CURLOPT_HEADER, true);
  curl_setopt($s, CURLOPT_NO_BODY, true);
  curl_setopt($s, CURLOPT_URL, $url);
  $header_string = curl_exec($s);
  if (preg_match('#Set-Cookie *: *(.*?)(?:\n|$)#i', $header_string, $m)) {
    $cookie = trim($m[1]);
  } else {
    erreur('Erreur serveur (cookie de session)', 320);
  }

  return $s;
}

// Post data and retrieve html/status
function do_post($s, $url, $post, &$cookie, &$status)
{
  curl_setopt($s, CURLOPT_COOKIE, $cookie);
  curl_setopt($s, CURLOPT_URL, $url);
  curl_setopt($s, CURLOPT_POST, true);
  curl_setopt($s, CURLOPT_HEADER, false);
  curl_setopt($s, CURLOPT_NO_BODY, false);
  curl_setopt($s, CURLOPT_FOLLOWLOCATION, true);
  curl_setopt($s, CURLOPT_POSTFIELDS, $post);

  $status = curl_getinfo($s, CURLINFO_HTTP_CODE);
  if (substr($status, 0, 1) != '2' && $status != 302) { // 2xx = "OK", 302 = "found"
    return false;
  }

  return curl_exec($s);
}






// Paramètre "gare"
if (!isset($_GET['gare'])) {
  erreur('Parametre "gare" attendu', 310);
} else {
  $nom_gare = $_GET['gare'];
}
// Paramètre "id"
if (isset($_GET['id'])) {
  $id_gare = intval($_GET['id']);
} else {
  $id_gare = null;
}
// Paramètre "nb"
$nb_departs = $nb_max_departs;
if (isset($_GET['nb'])) {
  $nb_departs = intval($_GET['nb']);
}
$nb_departs = min(20, max(5, $nb_departs));

// Test cache
$key = 'gare-'.str_replace(array("/", "\\", " ", "'"), array('', '', '', ''), $nom_gare) . '-ID-' . $id_gare;
if ($cache_enabled) {
  cache($key, $cache_timeout);
}

// Session cURL
$s = init_curl_cookie('http://www.termobile.fr/pages/imode/accueil.jsp', $cookie);

// Rechercher la gare
$html = do_post($s, 'http://www.termobile.fr/confirmerGareRPD.do', array('valeurGare' => $nom_gare), $cookie, $status);
if ($html === false) {
  erreur("Erreur serveur (status = $status)", 321);
}

// Extraire la liste des gares correspondant au nom demandé
$gares = array();
if (preg_match('#<select name=[\'"]?idxGare[\'"]?.*?>(.*?)</select>#is', $html, $m1)) {
  $options = $m1[1];
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
if (is_null($id_gare)) {
  retour(array('gares' => $gares), 201);
} elseif (!in_array($id_gare, $gares)) {
  erreur('ID invalide', 350, array('gares' => $gares, 'nom' => $nom_gare, 'id' => $id_gare));
} else {
  $nom_gare = array_search($id_gare, $gares, true);
  if ($nom_gare === false) {
    erreur('ID invalide (bis)', 351, array('gares' => $gares, 'nom' => $nom_gare, 'id' => $id_gare));
  }
}

// Demander les prochains départs pour cette gare
$html = do_post($s, 'http://www.termobile.fr/rechercherRPD.do', array('idxGare' => $id_gare, 'idxNombre' => $nb_departs), $cookie, $status);
if ($html === false) {
  erreur("Erreur serveur (status = $status)", 322);
}

// Analyser le résultat
$departs = array();
$html = str_replace(array("\r", "\n", '&nbsp;', "\t"), array('', '', ' ', ' '), $html);
preg_match_all('#<p (?:class|align)=\"right\">.*?<hr */>#i', $html, $m, PREG_SET_ORDER);
foreach ($m as $item) {
  $item = utf8_encode(trim($item[0]));
  $depart = array();
  if (preg_match('#Départ dans (.*?)<br#i', $item, $m2)) {
    $depart['attention'] = trim($m2[1]);
  }
  if (preg_match('#<(?:span|font)[^>]*>Mode *: *</(?:span|font)> *([^<>]+).*?<br[^>]*> *<(?:span|font)[^>]*>N° *: *</(?:span|font)> *(.*?) *<br#i', $item, $m2)) {
    $depart['type'] = trim($m2[1]);
    $depart['numero'] = trim($m2[2]);
  }
  if (preg_match('#<(?:span|font)[^>]*>Heure *: *</(?:span|font)> *(.*?) *<br#i', $item, $m2)) {
    $depart['heure'] = trim($m2[1]);
  }
  if (preg_match('#<(?:span|font)[^>]*>Destination *: *</(?:span|font)> *(.*?) *<br#i', $item, $m2)) {
    $depart['destination'] = nom_gare(trim($m2[1]));
  }
  preg_match_all('#<(?:span|font)[^>]*>Retard *: *</(?:span|font)>(.*?)<br(.*?Motif *:.*?<br)?#i', $item, $m2, PREG_SET_ORDER);
  $depart['retards'] = array();
  foreach ($m2 as $item2) {
    $item2 = $item2[0];
    $retard = array();
    if (preg_match('#(?:<(?:span|font)[^>]*>)?Retard *: *</(?:span|font)> *(.*?) *<br#i', $item2, $m3)) {
      $retard['retard'] = trim($m3[1]);
    } else {
      continue;
    }
    if (preg_match('#<(?:span|font)[^>]*>Motif *: *</(?:span|font)> *(.*?) *<br#i', $item2, $m3)) {
      $retard['motif'] = trim($m3[1]);
    }
    $depart['retards'][] = $retard;
  }
  $departs[] = $depart;
}
retour(array('nom' => $nom_gare, 'id' => $id_gare, 'departs' => $departs), 202);
