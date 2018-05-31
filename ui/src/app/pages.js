import React from "react";
import {Pagination} from "react-bootstrap";

export default class Pages extends React.Component {

    handleClick(page) {
        this.props.onClick(page);
    }

    render() {
        return (
            <Pagination>
                {[...Array(this.props.pageTotal)].map((x, i) => {
                    return (
                        <Pagination.Item
                            key={i}
                            onClick={() => this.handleClick(i)}
                            active={this.props.pageCurrent === i}
                        >
                            {i + 1}
                        </Pagination.Item>
                    );
                })}
            </Pagination>
        );
    }

}
