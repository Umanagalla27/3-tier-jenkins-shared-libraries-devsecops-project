const dbConfig = require('./DbConfig');
const mysql = require('mysql');

let con;

async function init() {
    const secrets = await dbConfig.getDbSecret();
    con = mysql.createConnection({
        host: secrets.DB_HOST,
        port: secrets.DB_PORT,
        user: secrets.DB_USER,
        password: secrets.DB_PWD,
        database: secrets.DB_DATABASE
    });

    return new Promise((resolve, reject) => {
        con.connect(function (err) {
            if (err) reject(err);
            console.log("Connected to MySQL!");
            resolve();
        });
    });
}

function addTransaction(amount, desc) {
    var sql = "INSERT INTO `transactions` (`amount`, `description`) VALUES (?, ?)";
    con.query(sql, [amount, desc], function (err, result) {
        if (err) throw err;
        console.log("Adding to the table should have worked");
    })
    return 200;
}

function getAllTransactions(callback) {
    var sql = "SELECT * FROM transactions";
    con.query(sql, function (err, result) {
        if (err) throw err;
        console.log("Getting all transactions...");
        return (callback(result));
    });
}

function findTransactionById(id, callback) {
    var sql = "SELECT * FROM transactions WHERE id = ?";
    con.query(sql, [id], function (err, result) {
        if (err) throw err;
        console.log(`retrieving transactions with id ${id}`);
        return (callback(result));
    })
}

function deleteAllTransactions(callback) {
    var sql = "DELETE FROM transactions";
    con.query(sql, function (err, result) {
        if (err) throw err;
        console.log("Deleting all transactions...");
        return (callback(result));
    })
}

function deleteTransactionById(id, callback) {
    var sql = "DELETE FROM transactions WHERE id = ?";
    con.query(sql, [id], function (err, result) {
        if (err) throw err;
        console.log(`Deleting transactions with id ${id}`);
        return (callback(result));
    })
}


module.exports = { init, addTransaction, getAllTransactions, deleteAllTransactions, findTransactionById, deleteTransactionById };







