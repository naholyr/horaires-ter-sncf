<?php

define('DEBUG', isset($_GET) && isset($_GET['debug']));

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
  return trim(preg_replace('/^gare d[e\'] */i', '', $nom));
}

// Retour erreur
class ClientError extends Exception {}
function erreur($message, $code, $additional_info = null)
{
  throw new ClientError(json_encode(array('code' => $code, 'error' => $message, 'info' => $additional_info)));
}
function exception_handler($exception)
{
  header('Content-type: ' . (DEBUG ? 'text/plain' : 'application/json'));
  echo $exception->getMessage();
  exit(0);
}
set_exception_handler('exception_handler');

// Retour succès
function retour($data, $code = 200)
{
  header('Content-type: ' . (DEBUG ? 'text/plain' : 'application/json'));
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
      header('Content-type: ' . (DEBUG ? 'text/plain' : 'application/json'));
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

// Client
function get_client($type)
{
  static $clients = array();
  if (!isset($clients[$type]))
  {
    require_once dirname(__FILE__) . '/client.' . $type . '.class.php';
    $class = 'client_' . $type;
    $clients[$type] = new $class();
  }
  return $clients[$type];
}
