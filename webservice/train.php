<?php

if (isset($_GET['source']) && $_GET['source'] == 1) { highlight_file(__FILE__); exit(0); }

// Configuration
$cache_timeout = 3600;
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
  if (!$header_string) {
    erreur('Erreur serveur termobile.fr inaccessible ! Réessayez plus tard', 400);
  }
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






// Paramètre "num"
$num = null;
if (!isset($_GET['num'])) {
  erreur('Parametre "num" attendu', 310);
} else {
  $num = $_GET['num'];
  if (!preg_match('/^[A-Za-z\-_0-9]+$/', $num)) {
    erreur('Parametre "num" invalide', 311);
  }
}

// Test cache
$key = 'train-'.$num;
if ($cache_enabled) {
  cache($key, $cache_timeout);
}

// Session cURL
$s = init_curl_cookie('http://www.termobile.fr/pages/imode/accueil.jsp', $cookie);

// Rechercher la gare
$html = do_post($s, 'http://www.termobile.fr/rechercherRMT.do', array('numTrain' => $num, 'dateTrain' => date('Y|m|d')), $cookie, $status);
if ($html === false) {
  erreur("Erreur serveur (status = $status)", 321);
}

// Extraire la liste des arrêts du train demandé
$info = array();
$html = str_replace(array("\r", "\n", '&nbsp;', "\t"), array('', '', ' ', ' '), $html);
if (preg_match('#<body> *(.*?) *</body>#i', $html, $match)) {
  $body = utf8_encode($match[1]);
  // Numéro
  if (preg_match('#N° *(.+?) *<#i', $body, $m)) {
    $info['numero'] = trim($m[1]);
  }
  // Date
  if (preg_match('#<(?:span|font)[^>]*>Date *: *</(?:span|font)> *(.*?) *<br#i', $body, $m)) {
    $info['date'] = trim($m[1]);
  }
  // Départ/Arrivée
  $info['heures'] = array();
  if (preg_match('#<(?:span|font)[^>]*>Départ *: *</(?:span|font)> *(.*?) *<br.*?<(?:span|font)[^>]*>Heure *: *</(?:span|font)> *(.*?) *<br#i', $body, $m)) {
    $info['heures'][nom_gare(trim($m[1]))] = trim($m[2]);
  }
  if (preg_match('#<(?:span|font)[^>]*>Destination *: *</(?:span|font)> *(.*?) *<br.*?<(?:span|font)[^>]*>Heure *: *</(?:span|font)> *(.*?) *<br#i', $body, $m)) {
    $info['heures'][nom_gare(trim($m[1]))] = trim($m[2]);
  }
  // Arrêts
  $info['arrets'] = array();
  if (($i = strpos($body, 'Arrêts desservis')) !== false) {
    $arrets = substr($body, $i);
    preg_match_all('#\* *(.+?)<br#i', $arrets, $mm, PREG_SET_ORDER);
    foreach ($mm as $arret) {
      $info['arrets'][] = trim(nom_gare($arret[1]));
    }
  }
} else {
  erreur("Mauvais format de la réponse", 330);
}
retour($info);
