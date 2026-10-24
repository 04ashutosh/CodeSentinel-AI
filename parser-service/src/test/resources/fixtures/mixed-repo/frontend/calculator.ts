import { config } from "./config";

export class Calculator {
    static calculateTax(subtotal: number, rate: number): number {
        return subtotal * rate;
    }
}