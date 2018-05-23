FROM node:9

RUN mkdir /usr/src/app
WORKDIR /usr/src/app

ENV PATH /usr/src/app/node_modules/.bin:$PATH
RUN npm install
RUN npm install react-scripts@1.1.1 -g

CMD ["npm", "start"]