//// Works fine... with all files... And converts into n-gram where: 1 <= n

#include<stdio.h>
#include<stdlib.h>
#include<wchar.h>
#include<wctype.h>
#include<locale.h>
#include<string.h>
#include<malloc.h>
#define length 500
#define TRUE 1
#define FALSE ~TRUE

struct node
{
	wchar_t word[length];
	int count;
	struct node *left, *right;
}*root=NULL;
typedef struct node node;

void treefromfile(char *);		// to make the binary search tree 
void addword(wchar_t *);	// to add a word in the tree
node* maketree(wchar_t *);	// at the 1st time 
void display(node *,FILE *);
void ngram(wchar_t *, int);


void treefromfile(char *filename)
{
	int i, n, cou, flag,j;
	node *temp;
	temp = (node *)malloc(sizeof(node));
	wchar_t str[length];
	wint_t c;
	long posi;
	FILE *pfile;
	
	pfile = fopen(filename,"r");
	
	if(pfile==NULL)
	{
		printf("File can't be opened\n");
		return;
	}
	
	printf("%s: file is opened\n",filename);
	printf("Enter the value of n to make the n-gram file: ");
	scanf("%d",&n);
	
	while(!feof(pfile))
	{
		for(cou=0 ; cou<n && !feof(pfile); )
		{
			i=0;
			flag=0;
        	        do {
                		c = fgetwc(pfile);
				if(c=='<')
				{
					i = 0;
					cou = 0;
					while(c!='>')
						c = fgetwc(pfile);
				}
				else if(c==WEOF)
				{
					ngram(str,i);
					printf("End of file\n");
					printf("%d-gram file created: named ngram_output.txt\n",n);
					return;
				}
				else if(c=='\n'||c=='\t'||c=='\''||c=='('||c==')'||c==','||c=='!'||c=='-'||c==';'||c=='?'||c=='\'')
					continue;

				else if(c==2404)	//for "Dari"
				{
					ngram(str,i);
					cou=0;
					posi=ftell(pfile);
					while((c=fgetwc(pfile))==' ')
					{
						posi=ftell(pfile);
					}
					break;
				}
				else if(c==' ')
				{
					cou++;
					if(cou==n)
					{
						ngram(str,i);
						break;
					}
					else
					{
						str[i++] = c;
						if(cou==1&&flag==0)
						{
							flag=1;
							posi = ftell(pfile);
						}
					}
				}
				else			
					str[i++] = c;
        	        } while(c!=WEOF&&cou<n);

			if(n != 1)
				fseek(pfile,posi,0);		
		}
	}
	
	fclose(pfile);
}


void ngram(wchar_t *str, int i)
{
      	str[i] = '\0';
	if(str[0]=='<'||str[0]=='\'')
		return;
	else
		addword(str);
}


void addword(wchar_t *w)
{
	node *p, *q;
	q = root;
	if(q==NULL)
	{
		root = maketree(w);
	}
	else
	{
		while(q!=NULL)
		{
			p = q;
			if(wcscmp(w,p->word)==0)
			{
				break;
			}
			if(wcscmp(w,q->word)<0)
			{
				q = q->left;
			}
			else
			{
				q = q->right;
			}
		}
		if(wcscmp(w,p->word)==0)
		{
			p->count++;
		}
		else if(wcscmp(w,p->word)<0)
		{
			p->left=maketree(w);
		}
		else
		{
			p->right=maketree(w);
		}

	}
}


node* maketree(wchar_t *w)
{
	node *p;
	p = (node *)malloc(sizeof(node));
	wcscpy(p->word,w);
	p->count = 1;
	p->left=NULL;
	p->right=NULL;
	return p;
}


void display(node *temp, FILE *op)
{
	if(temp!=NULL)
	{
		display(temp->left,op);
		fprintf(op,"%ls",temp->word);
		fprintf(op," : %d\n",temp->count);
		display(temp->right,op);
	}
}


main(int argc, char *argv[])
{
	FILE *op;
	
	if(argc !=2)
	{
		printf("Usage: Enter the name of the fie to be read\n");
		printf("Program terminated\n");
		exit(0);
	}
	
	op=fopen("ngram_output.txt","w");
	setlocale(LC_ALL,"");
	treefromfile(argv[1]);
	display(root,op);
	
	fclose(op);
	printf("\n");
}

