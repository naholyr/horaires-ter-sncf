<?php

include dirname(__FILE__).'/gares.config.php';

$dsn = 'mysql:host=localhost;dbname=horairestersncf';
$user = 'horairestersncf';
$pass = 'CdBFcEaGj9jJJdJV';

$sql_select = 'SELECT id, nom, region, adresse, latitude, longitude, updated_at FROM gares';
$sql_drop = 'DROP TABLE IF EXISTS gares';
$sql_create = 'CREATE TABLE gares (id INTEGER PRIMARY KEY AUTO_INCREMENT, nom VARCHAR(64) NOT NULL UNIQUE, region VARCHAR(32) NOT NULL, adresse VARCHAR(255) NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP())';
$sql_insert = 'INSERT INTO gares (nom, region, adresse, latitude, longitude) VALUES (:nom, :region, :adresse, :latitude, :longitude)';
$sql_where = '';

$db = new PDO($dsn, $user, $pass);
$db->query('SET NAMES UTF8');

if (array_key_exists('last_update', $_GET)) {
  $ts = strtotime($_GET['last_update']);
  if ($ts !== false) {
    $sql_where = 'WHERE updated_at > ' . $db->quote(date('Y-m-d H:i:s', $ts), PDO::PARAM_STR);
  }
}

if (!($stq = $db->query($sql_select.' '.$sql_where))) {
  $db->query($sql_drop);
  $db->query($sql_create);
  $sti = $db->prepare($sql_insert, array(PDO::ATTR_CURSOR => PDO::CURSOR_FWDONLY));
  $lines = file(dirname(__FILE__).'/gares.utf8.txt');
  foreach ($lines as $line) {
    list($nom, $region, $adresse, $latitude, $longitude) = array_map('trim', explode('#', trim($line)));
    $sti->execute(array('nom' => $nom, 'region' => $region, 'adresse' => $adresse, 'latitude' => floatval($latitude), 'longitude' => floatval($longitude)));
  }
  $stq = $db->query($sql_select);
}

if ($stq) {
  header('Content-Type: text/plain; charset=UTF-8');
  echo $db->query('SELECT COUNT(*) FROM gares '.$sql_where)->fetchColumn(), "\n";
  echo $db->query('SELECT MAX(updated_at) FROM gares')->fetchColumn(), "\n";
  foreach ($stq as $row) {
    echo implode('#', array($row['id'], $row['region'], $row['nom'], $row['adresse'], $row['latitude'], $row['longitude'])), "\n";
  }
}
