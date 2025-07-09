<?php

  // m'kay
  $drugsarebad = true;

  // if the cookie says we are a dealer then
  if($_COOKIE and array_key_exists(md5("dealer"), $_COOKIE) and
      $_COOKIE[md5("dealer")] == md5(1)) {
    $drugsarebad = false; // ignore mr mackey.
  }

  // has this user been killed?
  $dead = false;

  // if we are logged in ...
  if($_SESSION and $_SESSION['user']) {
    // get the sql database object
    require_once("sql.php");

    // find out
    $sql = "SELECT u.killed_by, u.killed_on FROM users u WHERE u.id = " . 
      $_SESSION['user']['id'] . " AND u.killed_on IS NOT NULL AND u.killed_by IS NOT NULL";
      $dead = $website_db->querySingle($sql, true);
      if ($dead === false)
        die($website_db->lastErrorMsg());

  }

  if(!empty($dead)  && $_SERVER['REQUEST_URI'] != '/dead.php') {
    header("Location: /dead.php");
    die();
  }
?>
