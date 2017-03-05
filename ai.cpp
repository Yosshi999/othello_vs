#include <iostream>
#include <map>
#include <windows.h>
using namespace std;

int canPutNum = 0;
pair<int, int> canPut[64];
char c[8][8];
int turn, li;	// turn:0->white, 1->black
char table[] = "ox";

bool search(int row, int line) {
	for (int moveR=-1; moveR<=1; moveR++) {
		for (int moveL=-1; moveL<=1; moveL++) {
			bool hasEnemy = false;
			if (moveL == 0 && moveR == 0) continue;

			for (int r=row, l=line; 0<=r && r<8 && 0<=l && l<8 ; r+=moveR, l+=moveL) {
				if (r==row && l==line) {
					// put point
					if (c[r][l] != '.') return false;
					else continue;
				}
				if (c[r][l] == table[(turn+1)%2]) {
					// enemy stone
					hasEnemy = true;
					continue;
				}
				if (c[r][l] == table[turn]) {
					// own stone
					if (hasEnemy) {
						return true;
					} else {
						// cant flip
						break;
					}
				}
				if (c[r][l] == '.') {
					break;
				}
			}
		}
	}
	return false;
}
int main(){
	while (1) {
		long t;
		cin >> t;
		cerr << t;

		cin >> turn >> li;

		for (int i=0; i<8; i++) {
			for (int j=0; j<8; j++) {
				cin >> c[i][j];
			}
		}
		int ite = 0;
		for (int i=0; i<8; i++) {
			for (int j=0; j<8; j++) {
				if (search(i,j)) {
					canPut[ite] = make_pair(i,j);
					ite++;
				}
			}
		}
		if (ite != 0) {
			// Sleep(100);
			cout << canPut[0].first << " " << canPut[0].second << endl;
		} else {
			cerr << "pass" << endl;
		}
	}
	return 0;
}
