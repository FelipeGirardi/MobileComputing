package com.example.calculator;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private enum Operation{
        PLUS,
        MINUS,
        DIVIDE,
        MULTIPLY,
        NONE
    }

    private Operation operation = Operation.NONE;
    private boolean firstInput = true;
    private String firstNum = "";
    private String secondNum = "";
    private TextView numDisplay;
    private String result = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numDisplay = findViewById(R.id.display);
    }

    public void onClickC(View view) {
        result = "0";
        firstInput = true;
        firstNum = "";
        secondNum = "";
        numDisplay.setText(result);
    }

    public void onClickEquals(View view) {
        if (firstInput){
            firstInput = false;
            operation = Operation.NONE;
            return;
        }

        if (firstNum == "" || secondNum == ""){
            return;
        }

        double first = Double.parseDouble(firstNum);
        double second = Double.parseDouble(secondNum);

        result = calcResult(first, second, operation);
        firstNum = result;
        secondNum = "";
        numDisplay.setText(result);
        operation = Operation.NONE;
    }


    private String calcResult(double first, double second, Operation op){
        double res;
        switch (op){
            case MULTIPLY:
                res = first*second;
                break;
            case DIVIDE:
                res = first/second;
                break;
            case MINUS:
                res = first-second;
                break;
            case PLUS:
                res = first+second;
                break;
            default:
                res = Double.parseDouble(result);
        }
        return Double.toString(res);
    }


    public void onClickComma(View view) {
        if (firstInput){
            firstNum = firstNum.concat(".");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat(".");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickZero(View view) {
        if (firstInput){
            firstNum = firstNum.concat("0");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("0");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickThree(View view) {
        if (firstInput){
            firstNum = firstNum.concat("3");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("3");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickTwo(View view) {
        if (firstInput){
            firstNum = firstNum.concat("2");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("2");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickPlus(View view) {
        operation = Operation.PLUS;
        firstInput = false;
        secondNum = "";
        numDisplay.setText(secondNum);
    }

    public void onClickOne(View view) {
        if (firstInput){
            firstNum = firstNum.concat("1");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("1");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickMinus(View view) {
        operation = Operation.MINUS;
        firstInput = false;
        secondNum = "";
        numDisplay.setText(secondNum);
    }

    public void onClickSix(View view) {
        if (firstInput){
            firstNum = firstNum.concat("6");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("6");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickFive(View view) {
        if (firstInput){
            firstNum = firstNum.concat("5");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("5");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickFour(View view) {
        if (firstInput){
            firstNum = firstNum.concat("4");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("4");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickEight(View view) {
        if (firstInput){
            firstNum = firstNum.concat("8");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("8");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickNine(View view) {
        if (firstInput){
            firstNum = firstNum.concat("9");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("9");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickMultiply(View view) {
        operation = Operation.MULTIPLY;
        firstInput = false;
        secondNum = "";
        numDisplay.setText(secondNum);
    }

    public void OnClickSeven(View view) {
        if (firstInput){
            firstNum = firstNum.concat("7");
            numDisplay.setText(firstNum);
        }
        else{
            secondNum = secondNum.concat("7");
            numDisplay.setText(secondNum);
        }
    }

    public void onClickDivide(View view) {
        operation = Operation.DIVIDE;
        firstInput = false;
        secondNum = "";
        numDisplay.setText(secondNum);
    }

    public void onClickC2(View view) {
        result = "0";
        firstInput = true;
        numDisplay.setText(result);
    }
}